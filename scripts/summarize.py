"""Resume resultados (JUnit XML, lint XML, log Gradle) e grava histórico em JSONL."""
import glob, json, os, re, sys, time
import xml.etree.ElementTree as ET

def local(log, rc, dur, hist, root):
    tests = fails = skipped = 0
    failures = []
    for f in glob.glob(os.path.join(root, "app/build/test-results/testDebugUnitTest/*.xml")):
        r = ET.parse(f).getroot()
        tests += int(r.get("tests", 0)); skipped += int(r.get("skipped", 0))
        for tc in r.iter("testcase"):
            for fl in list(tc.findall("failure")) + list(tc.findall("error")):
                fails += 1
                failures.append(f"{tc.get('classname').split('.')[-1]}.{tc.get('name')}: {(fl.get('message') or '')[:160]}")
    lint_err = None
    lx = os.path.join(root, "app/build/reports/lint-results-debug.xml")
    if os.path.exists(lx) and os.path.getmtime(lx) > time.time() - int(dur) - 5:
        issues = ET.parse(lx).getroot().findall("issue")
        lint_err = sum(1 for i in issues if i.get("severity") in ("Error", "Fatal"))
        lint_warn = len(issues) - lint_err
    apk = os.path.join(root, "app/build/outputs/apk/release/app-release.apk")
    apk_kb = os.path.getsize(apk) // 1024 if os.path.exists(apk) else None
    errs = []
    if int(rc) != 0:
        for line in open(log, errors="replace"):
            if re.match(r"^(e: |> (?!Task)|FAILURE|\* What went wrong|.*error:)", line):
                errs.append(line.rstrip()[:200])
    rec = dict(ts=time.strftime("%Y-%m-%dT%H:%M:%S"), kind="local", rc=int(rc), dur_s=int(dur),
               tests=tests, failures=fails, skipped=skipped, apk_kb=apk_kb, lint_errors=lint_err)
    open(hist, "a").write(json.dumps(rec) + "\n")
    status = "OK" if int(rc) == 0 else "FALHOU"
    print(f"[{status}] testes={tests} falhas={fails} apk={apk_kb}KB tempo={dur}s" +
          (f" lint: {lint_err} erros/{lint_warn} avisos" if lint_err is not None else ""))
    for f in failures[:10]: print("  FAIL", f)
    for e in errs[:15]: print("  ", e)
    print(f"  log: {log}")

if __name__ == "__main__":
    if sys.argv[1] == "local":
        local(*sys.argv[2:7])
