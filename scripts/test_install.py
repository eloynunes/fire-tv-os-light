from pathlib import Path
import os,subprocess,tempfile,unittest
ROOT=Path(__file__).resolve().parents[1]
class Installer(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);p=Path(self.tmp.name)
  self.log=p/'adb.log';self.env=os.environ.copy();self.env.update(DEVICE='test-target',APK=str(p/'app.apk'),FAKE_LOG=str(self.log))
  (p/'app.apk').write_bytes(b'fixture')
  for var,body in {
   'ADB':'echo "$*" >> "$FAKE_LOG"; exit "${FAKE_ADB_RC:-0}"',
   'APKSIGNER':'exit "${FAKE_SIGN_RC:-0}"',
   'AAPT':'''echo "package: name='${FAKE_PACKAGE:-com.firetvoslight.launcher}' versionCode='4'"'''
  }.items():
   f=p/var;f.write_text('#!/bin/bash\n'+body+'\n');f.chmod(0o755);self.env[var]=str(f)
 def run_install(self):return subprocess.run(['bash',str(ROOT/'scripts/install.sh')],env=self.env,capture_output=True,text=True)
 def assert_no_adb(self):self.assertFalse(self.log.exists())
 def test_success(self):
  p=self.run_install();self.assertEqual(p.returncode,0,p.stderr);self.assertEqual(self.log.read_text().count('\n'),1);self.assertIn('install -r',self.log.read_text());self.assertNotIn('settings',self.log.read_text())
 def test_install_failure_not_hidden(self):
  self.env['FAKE_ADB_RC']='7';p=self.run_install();self.assertEqual(p.returncode,7);self.assertNotIn('Update accepted',p.stdout);self.assertEqual(self.log.read_text().count('\n'),1)
 def test_invalid_signature(self):
  self.env['FAKE_SIGN_RC']='9';self.assertEqual(self.run_install().returncode,9);self.assert_no_adb()
 def test_wrong_package(self):
  self.env['FAKE_PACKAGE']='other.package';self.assertEqual(self.run_install().returncode,2);self.assert_no_adb()
 def test_missing_target(self):
  self.env.pop('DEVICE');self.assertNotEqual(self.run_install().returncode,0);self.assert_no_adb()
 def test_missing_apk(self):
  self.env['APK']+='.missing';self.assertEqual(self.run_install().returncode,2);self.assert_no_adb()
if __name__=='__main__':unittest.main()
