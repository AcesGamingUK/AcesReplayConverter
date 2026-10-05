from pathlib import Path
import zipfile, json, hashlib, shutil

BASE = Path(__file__).resolve().parent.parent
OUT = BASE / 'outputs' / 'release-1.0.0'
OUT.mkdir(parents=True, exist_ok=True)
SOURCE = BASE / 'outputs' / 'AcesReplayConverter-0.1.4-dev.jar'
IMAGE = Path('C:/Users/shane/.codex/generated_images/01a0e21b-6422-7e82-a28a-492f992a6889/exec-b03bf618-b50e-4852-ae79-7bb37634fb71.png')
shutil.copyfile(IMAGE, OUT / 'AcesReplayConverter-project-image.png')
icon = IMAGE.read_bytes()
jar_path = OUT / 'AcesReplayConverter-1.0.0.jar'
with zipfile.ZipFile(SOURCE) as src, zipfile.ZipFile(jar_path, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
    for entry in src.infolist():
        data = src.read(entry.filename)
        if entry.filename == 'fabric.mod.json':
            meta = json.loads(data)
            assert meta['version'] == '0.1.4-dev'
            meta['version'] = '1.0.0'
            meta['description'] = 'Convert native Minecraft 1.21.1 ReplayMod MCPR recordings into Flashback replays from the replay selection screen. Includes handling for recorded Simple Voice Chat data. Built for protocol 767 and Flashback 0.39.10.'
            data = json.dumps(meta, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
        elif entry.filename == 'assets/flashback-converter/icon.png':
            data = icon
        dst.writestr(entry, data)

with zipfile.ZipFile(SOURCE) as src, zipfile.ZipFile(jar_path) as release:
    assert src.namelist() == release.namelist()
    assert release.testzip() is None
    changes = [n for n in src.namelist() if src.read(n) != release.read(n)]
    assert sorted(changes) == ['assets/flashback-converter/icon.png', 'fabric.mod.json']
    classes = [n for n in src.namelist() if n.endswith('.class')]
    assert all(src.read(n) == release.read(n) for n in classes)
    assert release.read('META-INF/jars/viaversion-common-5.9.1.jar') == src.read('META-INF/jars/viaversion-common-5.9.1.jar')
    for n in ['LICENSE-CC0', 'LICENSE_flashback-converter', 'THIRD_PARTY_NOTICES.md']:
        (OUT / n).write_bytes(release.read(n))

checksum = hashlib.sha256(jar_path.read_bytes()).hexdigest()
(OUT / 'SHA256SUMS.txt').write_text(f'{checksum}  {jar_path.name}\n', encoding='utf-8')
report = {'release': '1.0.0', 'base': '0.1.4-dev', 'sha256': checksum,
          'changed_archive_entries': changes, 'unchanged_class_files': len(classes),
          'archive_crc_check': 'PASS', 'nested_dependencies': 'byte-for-byte unchanged',
          'previous_validation': 'Java 21 strict JVM verification, ASM verification, repeated native lifecycles and 257694-packet test conversion; user reported playback working',
          'scope': 'Release packaging; version, metadata description and icon only. Existing conversion limitations remain.'}
(OUT / 'release-verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
print(json.dumps(report, indent=2))
