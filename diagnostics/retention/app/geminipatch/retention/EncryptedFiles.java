package app.geminipatch.retention;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Ciphertext-only atomic writes in no-backup app storage; key never leaves AndroidKeyStore. */
final class EncryptedFiles {
 private static final String ALIAS="geminipatch.saved-replies.v1";
 final File directory;
 EncryptedFiles(Context c){directory=new File(c.getNoBackupFilesDir(),"geminipatch-saved-replies");}
 private SecretKey key(boolean create)throws Exception{
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return(SecretKey)store.getKey(ALIAS,null);
  if(!create)throw new IOException();
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
   .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
  return generator.generateKey();
 }
 private File file(String name){if(!name.matches("index\\.enc|[a-f0-9]{32}\\.enc"))throw new IllegalArgumentException();return new File(directory,name);}
 boolean exists(String name){File f=file(name);return f.exists()||new File(f.getPath()+".bak").exists();}
 synchronized void write(String name,byte[] plain)throws Exception{
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException();
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(true));cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
  byte[] iv=cipher.getIV(),encrypted=cipher.doFinal(plain);if(iv.length!=12)throw new IOException();
  AtomicFile file=new AtomicFile(file(name));FileOutputStream stream=file.startWrite();
  try{DataOutputStream out=new DataOutputStream(stream);out.writeInt(0x47505231);out.write(iv);out.write(encrypted);out.flush();file.finishWrite(stream);}
  catch(Throwable error){file.failWrite(stream);throw error;}
 }
 synchronized byte[] read(String name,int maximum)throws Exception{
  AtomicFile file=new AtomicFile(file(name));
  try(FileInputStream input=file.openRead()){
   if(input.getChannel().size()>maximum+64||input.getChannel().size()<32)throw new IOException();
   DataInputStream in=new DataInputStream(input);if(in.readInt()!=0x47505231)throw new IOException();byte[] iv=new byte[12];in.readFully(iv);
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
   while((count=in.read(buffer))!=-1){if(out.size()+count>maximum+16)throw new IOException();out.write(buffer,0,count);}
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,iv));cipher.updateAAD(name.getBytes(StandardCharsets.UTF_8));
   return cipher.doFinal(out.toByteArray());
  }
 }
 synchronized void delete(String name){new AtomicFile(file(name)).delete();}
 synchronized void clear()throws IOException{File[] files=directory.listFiles();if(files==null)return;for(File f:files)if(f.isFile()&&f.getName().matches("(?:index|[a-f0-9]{32})\\.enc(?:\\.bak|\\.new)?")&&!f.delete()&&f.exists())throw new IOException();}
}
