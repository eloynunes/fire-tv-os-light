"""Gera resumo curto do diagnóstico bruto (somente dados reais coletados)."""
import os, re, sys
d = sys.argv[1]
def rd(n):
    p = os.path.join(d, n)
    return open(p, errors="replace").read() if os.path.exists(p) else ""
props = dict(re.findall(r"\[(.+?)\]: \[(.*?)\]", rd("getprop.txt")))
mi = dict((k, int(v)) for k, v in re.findall(r"^(\w+):\s+(\d+) kB", rd("meminfo.txt"), re.M))
cpu = rd("cpuinfo.txt")
cores = len(re.findall(r"^processor\s*:", cpu, re.M))
hw = (re.findall(r"^Hardware\s*:\s*(.+)$", cpu, re.M) or ["?"])[0]
freqs = [int(x) for x in re.findall(r" (\d{6,})", rd("cpufreq.txt"))]
tot = re.search(r"Total RAM:\s*([\d,]+)K", rd("dumpsys_meminfo.txt"))
pss = re.findall(r"^\s*([\d,]+)K: (\S+) \(pid", rd("dumpsys_meminfo.txt").split("Total PSS by OOM")[0], re.M)[:8]
gpu = re.search(r"GLES: (.+)", rd("surfaceflinger.txt"))
data = [l for l in rd("df.txt").splitlines() if l.rstrip().endswith("/data")]
home = rd("home.txt").strip().splitlines()
pk3 = len(rd("packages3.txt").splitlines()); pk = len(rd("packages.txt").splitlines())
lb = len(re.findall(r"^\s*Activity #", rd("leanback.txt"), re.M)) or rd("leanback.txt").count("ActivityInfo")
g = lambda k: props.get(k, "?")
print(f"""# Diagnóstico real — {os.path.basename(d)}
- Modelo: {g('ro.product.model')} ({g('ro.product.device')}) | Fabricante: {g('ro.product.manufacturer')}
- Fire OS: {g('ro.build.version.name')} | build {g('ro.build.version.number')} | Android {g('ro.build.version.release')} / API {g('ro.build.version.sdk')}
- ABI: {g('ro.product.cpu.abilist')} | SoC: {g('ro.board.platform')} / {hw} | núcleos: {cores} | freq máx: {max(freqs)//1000 if freqs else '?'} MHz
- RAM total: {mi.get('MemTotal',0)//1024} MB | disponível: {mi.get('MemAvailable',0)//1024} MB | livre: {mi.get('MemFree',0)//1024} MB | swap livre: {mi.get('SwapFree',0)//1024}/{mi.get('SwapTotal',0)//1024} MB
- GPU: {gpu.group(1).strip() if gpu else '?'}
- Tela: {' '.join(rd('wm.txt').split())}
- /data: {data[0] if data else '?'}
- Pacotes: {pk} total, {pk3} de terceiros | atividades LEANBACK: {lb}
- Home resolvida: {home[-1].strip() if home else '?'}
- Maiores processos (PSS): {', '.join(f'{n} {s}K' for s, n in pss)}
- Heap por app: {g('dalvik.vm.heapgrowthlimit')} (limite {g('dalvik.vm.heapsize')}) | low_ram: {g('ro.config.low_ram')}""")
