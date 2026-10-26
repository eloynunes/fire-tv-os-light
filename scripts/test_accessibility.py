import importlib.util, pathlib, unittest
spec=importlib.util.spec_from_file_location('helper',pathlib.Path(__file__).with_name('accessibility.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class Services(unittest.TestCase):
 def test_enable_preserves_others(self):self.assertEqual(m.update_services('org.reader/.Service',True),'org.reader/.Service:'+m.SERVICE)
 def test_disable_preserves_others(self):self.assertEqual(m.update_services('org.reader/.Service:'+m.SERVICE,False),'org.reader/.Service')
 def test_short_component_removed(self):self.assertEqual(m.update_services(m.PKG+'/.HomeRedirectService',False),'')
 def test_enable_idempotent(self):self.assertEqual(m.update_services(m.SERVICE,True),m.SERVICE)
 def test_null(self):self.assertEqual(m.update_services('null',False),'')
 def test_empty_enable(self):self.assertEqual(m.update_services('',True),m.SERVICE)
 def test_reject_malformed(self):
  for value in ('unexpected','reader/.S::other/.T','x/.A;echo bad'):
   with self.subTest(value=value),self.assertRaises(ValueError):m.update_services(value,True)
if __name__=='__main__':unittest.main()
