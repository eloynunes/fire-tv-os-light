#!/usr/bin/env python3
"""Change only TV Light's entry in the accessibility-service list."""
import argparse, os, re, subprocess
PKG = 'com.firetvoslight.launcher'
SERVICE = PKG + '/' + PKG + '.HomeRedirectService'
def is_ours(value):
    return value in (SERVICE, PKG + '/.HomeRedirectService')
def update_services(raw, enable):
    values = [] if raw.strip() in ('', 'null') else raw.strip().split(':')
    if any(not re.fullmatch(r'[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+', x) for x in values):
        raise ValueError('Unexpected accessibility-service list; refusing to overwrite it.')
    result = [x for x in values if not is_ours(x)]
    if enable: result.append(SERVICE)
    return ':'.join(result)
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action',choices=('on','off','status'))
    parser.add_argument('--serial',required=True)
    args=parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_.:\[\]-]{1,200}',args.serial):parser.error('Invalid serial')
    def adb(*command):
        p=subprocess.run(['adb','-s',args.serial,*command],capture_output=True,text=True,timeout=15,check=True)
        return p.stdout.strip()
    def read(key):return adb('shell','settings','get','secure',key)
    old=read('enabled_accessibility_services'); flag=read('accessibility_enabled')
    if flag not in ('0','1','null'):raise ValueError('Unknown accessibility state')
    if args.action!='status':
        new=update_services(old,args.action=='on')
        # Refuse an observed concurrent change. Settings updates are not atomic.
        if read('enabled_accessibility_services')!=old:raise ValueError('Service list changed; retry manually.')
        if new:
            adb('shell','settings','put','secure','enabled_accessibility_services',new)
        else:
            adb('shell','settings','delete','secure','enabled_accessibility_services')
        if args.action=='on':adb('shell','settings','put','secure','accessibility_enabled','1')
        elif not new:adb('shell','settings','put','secure','accessibility_enabled','0')
        observed=read('enabled_accessibility_services')
        if ('' if observed=='null' else observed)!=new:raise ValueError('Readback differs; inspect device before retrying.')
    print('Services: '+read('enabled_accessibility_services'))
    print('Accessibility enabled: '+read('accessibility_enabled'))
if __name__=='__main__':
    try:main()
    except (ValueError,OSError,subprocess.SubprocessError) as error:
        raise SystemExit('Stopped: '+str(error)+'; no automatic retry. A partial settings change may have been applied.')
