"""Resume o teste no aparelho e restaura o favorito alterado pelo teste de persistência."""
import glob, json, os, re, subprocess, sys, time
out, hist = sys.argv[1], sys.argv[2]
rd = lambda n: open(os.path.join(out, n), errors="replace").read() if os.path.exists(os.path.join(out, n)) else ""
def total(f): m = re.search(r"TotalTime: (\d+)", rd(f)); return int(m.group(1)) if m else None
cold = [t for t in (total(f"start-cold-{i}.txt") for i in (1, 2, 3)) if t is not None]
warm = total("start-warm.txt")
mem = re.search(r"TOTAL(?: PSS)?:?\s+(\d+)", rd("meminfo.txt"))
pss_kb = int(mem.group(1)) if mem else None
priv = re.search(r"^\s+TOTAL\s+\d+\s+(\d+)\s+(\d+)", rd("meminfo.txt"), re.M)
priv_kb = int(priv.group(1)) + int(priv.group(2)) if priv else None
log = rd("logcat.txt")
crash = "FATAL EXCEPTION" in log and "com.firetvoslight.launcher" in log
anr = "ANR in com.firetvoslight.launcher" in log
first = re.findall(r"firstRenderMs=(\d+)", log); loadms = re.findall(r"apps=(\d+) loadMs=(\d+)", log)
jank = re.search(r"Janky frames: (\d+) \(([\d.]+)%\)", rd("gfxinfo.txt"))
apps_n = len(re.findall(r'content-desc="App: ', rd("before.xml")))
f0, f1, f2, f3 = (rd(f"s{i}.txt").strip() for i in range(4))
persist_ok = bool(f0) and f1 != f0 and f1 == f2
rec = dict(ts=time.strftime("%Y-%m-%dT%H:%M:%S"), kind="device", cold_ms=cold, warm_ms=warm, pss_kb=pss_kb, private_kb=priv_kb,
           first_render_ms=[int(x) for x in first], load=loadms[:3], jank=jank.group(0) if jank else None,
           crash=crash, anr=anr, apps_visible=apps_n, persist_ok=persist_ok, restored=f3 == f0)
open(hist, "a").write(json.dumps(rec) + "\n")
ok = not crash and not anr and bool(cold) and persist_ok and f3 == f0 and apps_n > 0
print(f"[{'OK' if ok else 'FALHOU'}] frio(ms)={cold} quente={warm} PSS={pss_kb}KB privada={priv_kb}KB 1ºrender={first[:3]} load={loadms[:2]}")
print(f"  apps visíveis={apps_n} favoritos={f0.split(' ')[0]} | persistência={'OK' if persist_ok else 'FALHOU'} | restaurado={f3 == f0}")
print(f"  crash={crash} anr={anr} {rec['jank'] or ''}")
if crash:
    for l in log.splitlines():
        if "AndroidRuntime" in l: print("   ", l[:200])
sys.exit(0 if ok else 1)
