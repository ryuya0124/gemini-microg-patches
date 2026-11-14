"""Separate diagnostic packaging; never changes the stable feed or release directory."""
from pathlib import Path
from datetime import datetime, timezone
import hashlib,json,re,sys,zipfile,shutil

root=Path(__file__).resolve().parents[1]
metadata=json.loads((root/'diagnostics/metadata.json').read_text())
assert re.fullmatch(r'\d+\.\d+\.\d+-(?:diagnostics|preview)\.\d+',metadata['version'])
profile=json.loads((root/'diagnostics/profiles'/metadata['profile']).read_text())
assert profile['build']==metadata['version']
out=Path(sys.argv[2]).resolve()
assert out==root/'build/diagnostics'
mode=sys.argv[1]
def quote(s):return json.dumps(s,ensure_ascii=True)
def write_jar(path,entries):
    with zipfile.ZipFile(path,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(entries.items()):
            info=zipfile.ZipInfo(name,date_time=(2000,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16
            archive.writestr(info,data)
if mode=='schema':
    # Fresh generated classes/DEX prevent stale anonymous classes entering the release.
    for relative in ['runtime/classes','runtime/dex','patch-dex']:
        generated=(out/relative).resolve()
        assert generated.is_relative_to(out) and generated!=out
        if generated.exists():shutil.rmtree(generated)
        generated.mkdir(parents=True)
    lines=['package app.geminipatch.diagnostics;','import java.util.*;','final class Schema {']
    for key,value in [('BUILD',profile['build']),('VERSION',profile['versionName']),('STATUS',profile['statusType']),('ERROR_ENUM',profile['errorEnum']),('CAPABILITIES',profile['capabilitiesType']),('PUNT',profile['puntType']),('PUNT_TITLE',profile['puntTitleField']),('PUNT_BODY',profile['puntBodyField']),('PUNT_BEHAVIOR',profile['puntBehaviorField'])]:
        lines.append('static final String '+key+' = '+quote(value)+';')
    lines.append('static final long CODE = '+profile['versionCode']+'L;')
    lines.append('static final Map<String,String> ROOTS; static { Map<String,String> m=new HashMap<String,String>();')
    for k,v in profile['incomingTypes'].items():lines.append('m.put('+quote(k)+','+quote(v)+');')
    lines.extend(['ROOTS=Collections.unmodifiableMap(m);','}'])
    lines.append('static final Map<String,String> PUNT_FIELDS; static { Map<String,String> m=new HashMap<String,String>();')
    for k,v in profile['puntFields'].items():lines.append('m.put('+quote(k)+','+quote(v)+');')
    lines.extend(['PUNT_FIELDS=Collections.unmodifiableMap(m);','}','}'])
    (out/'runtime/Schema.java').write_text('\n'.join(lines)+'\n')
    (out/'resources/diagnostics/profile.json').write_bytes((root/'diagnostics/profiles'/metadata['profile']).read_bytes())
    # Use only the Java standard library in Android Manager's external patch host.
    # JSON remains the owning profile; this derived resource has no new runtime dependency.
    properties={k:v for k,v in profile.items() if isinstance(v,str)}
    for section in ['methods','incomingTypes','expectedSites']:
        properties.update({section+'.'+k:str(v) for k,v in profile[section].items()})
    properties['versionFields.count']=str(len(profile['versionFields']))
    for i,(field,observer) in enumerate(profile['versionFields'].items()):
        properties['versionFields.'+str(i)+'.field']=field
        properties['versionFields.'+str(i)+'.observer']=observer
    properties['capabilityFields.count']=str(len(profile['capabilityFields']))
    for i,field in enumerate(profile['capabilityFields']):properties['capabilityFields.'+str(i)]=field
    for k,v in properties.items():
        assert re.fullmatch(r'[A-Za-z0-9_.]+',k) and v.isascii() and not any(c in v for c in '\r\n\\')
    (out/'resources/diagnostics/profile.properties').write_text(''.join(k+'='+v+'\n' for k,v in sorted(properties.items())),encoding='ascii')
elif mode=='merge':
    entries={}
    for source in [root/'build/gemini-patches.jar',out/'addon.jar']:
        with zipfile.ZipFile(source) as archive:
            for n in archive.namelist():
                if n.endswith('/') or n=='META-INF/MANIFEST.MF':continue
                assert n not in entries,n
                entries[n]=archive.read(n)
    for f in (out/'resources').rglob('*'):
        if f.is_file():entries[f.relative_to(out/'resources').as_posix()]=f.read_bytes()
    write_jar(out/'diagnostics-patches.jar',entries)
elif mode=='package':
    version=metadata['version'];tag='v'+version;name='gemini-microg-patches-'+version+'.mpp'
    description=metadata.get('description','Opt-in local diagnostics for conversation update warnings; not a bug fix')
    attributes={'Manifest-Version':'1.0','Name':'Gemini microG patches','Description':description,'Version':version,'Timestamp':str(int(datetime.fromisoformat(metadata['created_at']).replace(tzinfo=timezone.utc).timestamp()*1000)),'Source':'https://github.com/ryuya0124/gemini-microg-patches','Author':'ryuya0124','Website':'https://github.com/ryuya0124/gemini-microg-patches','Patcher-Version':'1.12.0'}
    lines=[]
    for k,v in attributes.items():
        line=k+': '+v;line.encode('ascii')
        while len(line)>70:lines.append(line[:70]);line=' '+line[70:]
        lines.append(line)
    entries={'META-INF/MANIFEST.MF':('\r\n'.join(lines)+'\r\n\r\n').encode('ascii')}
    with zipfile.ZipFile(out/'diagnostics-patches.jar') as archive:
        entries.update({n:archive.read(n) for n in archive.namelist() if not n.endswith('/') and n!='META-INF/MANIFEST.MF'})
    dex=list(sorted((out/'patch-dex').glob('classes*.dex')));assert dex and dex[0].name=='classes.dex'
    for f in dex:entries[f.name]=f.read_bytes()
    release=out/'release';release.mkdir(exist_ok=True)
    bundle=release/name;write_jar(bundle,entries)
    feed={'version':version,'created_at':metadata['created_at'],'description':description,'download_url':'https://github.com/ryuya0124/gemini-microg-patches/releases/download/'+tag+'/'+name,'page_url':'https://github.com/ryuya0124/gemini-microg-patches/releases/tag/'+tag}
    (release/'diagnostics-bundle.json').write_text(json.dumps(feed,indent=2)+'\n')
    (release/'SHA256SUMS').write_text(''.join(hashlib.sha256(f.read_bytes()).hexdigest()+'  '+f.name+'\n' for f in [bundle,release/'diagnostics-bundle.json']))
    print('Packaged',name)
else:raise SystemExit('Unknown packaging stage')
