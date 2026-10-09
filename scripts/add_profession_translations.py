"""Append professional trial translations, preserving every existing catalog entry."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/com/goosethings/tools/client/task/ProfessionScreen.java").read_text(encoding="utf-8")
feedback_source = (root / "src/main/java/com/goosethings/tools/task/profession/ProfessionFeedback.java").read_text(encoding="utf-8")
common_zh = {
 "number":"计算结果", "subtitle":"专业仿真 · 指令试玩", "handbook":"知识手册", "waiting":"等待服务器……",
 "complete":"四个阶段全部通过", "result":"用时 %s 秒 · 错误 %s 次", "retry":"生成新题", "exit":"关闭界面",
 "controls":"选择卡片或调节器；方向键微调，Shift 加大步进，拖动滑条；Enter 验证。",
 "test":"测试", "check":"验证", "numeric":"请在所需输入框中填写有限的十进制数。", "dragging":"放置卡片", "back":"返回操作台",
}
feedback_zh = {
 "ready":"读取题面数据，配置当前阶段。", "calc_a":"第一项计算与所选配置不符；请检查公式、符号和单位。",
 "calc_b":"第二项计算不正确；请检查单位与题面误差范围。", "calc_c":"第三项计算不正确；请检查符号、单位与含水率基准。",
 "missing":"请先配置所有必需组件。", "optical":"接收功率须达到灵敏度＋3 dB，且不能超过 −13 dBm 上限。",
 "modulation":"所选调制未达到题面设备的信噪比门限。", "bandwidth":"占用带宽超过分配频谱。",
 "throughput":"净速率须达到需求，且不超过理论容量的 80%。", "iq":"校准相位、增益及 I/Q 直流偏移，使 EVM≤3.5%。",
 "hold":"请在合格窗口内连续稳定 0.8 秒，再验证。", "interference":"有连线的相邻站点须至少相隔 10 MHz。",
 "circuit":"识别热源、换热器与最终热阱；一次和二次回路流体必须隔离。",
 "subcritical":"请正确判断中子产生／损失比；次临界状态仍有停堆余热。",
 "pump":"选择满足 10% 流量余量的最小泵额定值。",
 "independence":"A/B 须使用不同电源母线与不同热阱，两路阀门均开启。",
 "cooling":"每路须独立移除题面余热的 110%–125%，流量不得超过所选泵额定值。",
 "testing":"正在测试双路正常、A 路失效、B 路失效；温度须保持题面窗口。",
 "temperature":"冷却测试温度超出题面 80–95 °C 窗口，请调整配置后重试。",
 "dose":"完成要求的最少作业时间，并将累计剂量控制在题面预算内。",
 "hazard":"将危害对应到题面已验证的控制措施，区分 GHP 与 CCP。",
 "lethality":"产品冷点累计致死量须达到目标，并满足工艺质量上限。",
 "contamination":"分开原料、即食与过敏原流向，使用专用工具和已验证清洁。",
 "trace":"按题面方案处置：异常原料后代暂扣、不可逆化学残留报废、已验证热处理偏差返工，其余合规批次放行。",
 "level":"按等长测段分配负闭合差，检查后视／前视的符号。",
 "diagram":"剪力图左端为＋R、右端为−R；跨中弯矩须与均布荷载一致。",
 "section":"应力、挠度和质量须同时合格，不能只检查强度。",
 "concrete":"按烘干质量基准修正配料，并选择养护及强度均合格的报告。", "success":"四个阶段全部通过。",
}
zh = {
 "telecom": {
  "title":"电信专业：通信站链路恢复", "optical_data":"光纤 %s km×0.25 dB/km；连接器 2×0.5 dB；熔接点 4×0.1 dB",
  "fiber_loss":"无源链路总损耗：%s dB", "rx_limits":"接收灵敏度 −26 dBm；余量 3 dB；上限 −13 dBm。功率之差使用 dB。",
  "tx":"发射功率 / dBm", "attenuator":"衰减器 / dB", "capacity_data":"带宽 %s MHz；SNR %s dB；需求 %s Mbit/s；净速率≤0.8C；滚降 0.25；开销 10%。",
  "mcs_table":"题面设备门限：QPSK≥6 dB；16-QAM≥14 dB。C=B log₂(1＋线性 SNR)。", "throughput":"净速率 %s Mbit/s · 占用 %s MHz",
  "modulation":"调制方式", "code_rate":"编码率", "symbol_rate":"符号率 / Msym/s", "evm":"EVM %s%% · 稳定 %s / 800 ms",
  "phase":"相位修正 / °", "gain":"增益倍数", "dc_i":"I 路直流修正", "dc_q":"Q 路直流修正",
  "channels":"频道：900／905／910／915 MHz；有连线的站点须至少相隔 10 MHz。",
 },
 "nuclear": {
  "title":"核电专业：停堆余热与辐射防护", "neutrons":"题面中子产生 %s／损失 %s；识别热流路径并判断 k_eff。",
  "role":"热工角色", "fluids":"一次／二次回路流体", "criticality":"中子状态", "component0":"反应堆", "component1":"蒸汽发生器", "component2":"冷却池",
  "decay_data":"题面曲线 P(t)=%s／(1＋t/60)^0.2 MW；求 t=%s 分钟时的余热。", "curve_axes":"t：0–120 分钟 · 余热 / MW",
  "cooling_data":"cp=%s kJ/(kg·K)；ΔT=%s K。Q=ṁcpΔT。流量留 10% 余量，选择最小合格泵。",
  "pump":"泵额定流量", "redundancy":"每路移除 110%–125% 余热；泵额定 %s kg/s；流量不得超限；电源与热阱独立。",
  "bus":"电源", "sink":"热阱", "valve":"阀门", "flow0":"A 路流量 / kg/s", "flow1":"B 路流量 / kg/s",
  "test_status":"温度 %s °C · %s / 6000 ms · 运行支路 %s",
  "dose_data":"点源：1 m 处 %s µSv/h；作业至少 %s 分钟；预算 %s µSv。",
  "dose_segment":"第 %s 段：%s m · 占总时间 %s%%", "route":"作业路线", "shield":"屏蔽透射系数", "worktime":"总作业时间 / 分钟",
 },
 "foodsafety": {
  "title":"食安专业：生产线控制与批次放行", "hazard_data":"题面：热处理及末端金检已验证；清洁控制化学残留和过敏原残留。",
  "hazard0":"原料病原体", "hazard1":"加工后金属碎屑", "hazard2":"换线含乳过敏原", "hazard3":"清洁剂残留",
  "control0":"GHP 进货／冲洗", "control1":"CCP 热处理", "control2":"CCP 金属探测", "control3":"GHP 清洁验证",
  "thermal_data":"Tref=70 °C；Dref=%s 分钟；z=%s °C；目标 %s log 降低；F 质量上限为目标的 3 倍。",
  "coldpoint":"绿线：产品冷点；黄线：加热器高 4 °C", "cold_set":"冷点平台温度 / °C", "holdtime":"平台保温时间 / 分钟",
  "thermal_samples":"前四段：50、60、T−6、T−2 °C，各 0.5 分钟；后两段均为 T °C，各占保温时间一半。按矩形段积分；T≤82 °C。",
  "zone0":"原料区", "zone1":"即食区", "zone2":"过敏原区", "tool0":"生鸡肉", "tool1":"已包装即食食品", "tool2":"含乳原料", "tool3":"原料区刀具",
  "cleaning":"换线清洁方法", "tools":"工具流向", "trace_data":"原料批 %s 异常：后代暂扣；不可逆化学残留报废；热偏差按已验证方案返工；其余记录合规。",
  "clean_report":"题面残留检验：冲洗阳性、验证清洁阴性、加热阳性；工具须专用。",
  "batch":"成品批 %s · 原料 %s", "deviation1":"热处理偏差；返工已验证", "deviation2":"化学残留；无法返工",
 },
 "civil": {
  "title":"土木专业：桥梁验算与施工质控", "survey_data":"起点及终点已知高程 %s m；三段等长；闭合差＝终点测得高程−已知高程。",
  "setup":"测站 %s", "correction":"闭合差校正方案", "beam_data":"简支梁：q=%s kN/m，L=%s m；调节两端剪力与跨中弯矩。",
  "shear":"剪力 V / kN", "moment":"弯矩 M / kN·m", "left_shear":"左端剪力 / kN", "right_shear":"右端剪力 / kN", "mid_moment":"跨中弯矩 / kN·m",
  "section_data":"E=200 GPa；σ≤160 MPa；δ≤L/250=%s mm；质量≤180 kg/m；q=%s kN/m；L=%s m。",
  "section":"选择截面 %s", "concrete_data":"有效水胶比 0.45；胶凝材料 %s kg；题面验收要求：强度≥30 MPa、养护≥7 天。",
  "aggregate0":"细骨料", "aggregate1":"粗骨料", "moisture_basis":"mc 和 a 均以烘干质量为基准。湿骨料与加水量分别修正；吸收水不计入有效用水。",
  "report":"报告 %s：%s MPa／%s 天",
 },
}
stages_zh={"telecom":["1 · 光链路预算","2 · 调制与容量","3 · IQ 校准","4 · 频谱规划"],
 "nuclear":["1 · 回路识图","2 · 余热核算","3 · 冗余验证","4 · 辐射规划"],
 "foodsafety":["1 · 危害分析","2 · 热处理验证","3 · 分区隔离","4 · 批次追溯"],
 "civil":["1 · 水准测量","2 · 梁内力","3 · 截面验算","4 · 配料验收"]}
fields={
 "telecom":[[("Received power / dBm","接收功率 / dBm"),("Receiver margin / dB","接收余量 / dB")],[("Shannon C / Mbit/s","香农容量 / Mbit/s"),("Selected net rate / Mbit/s","所选净速率 / Mbit/s")],[],[]],
 "nuclear":[[("k_eff","k_eff")],[("Decay heat / MW","余热 / MW"),("Flow without reserve / kg/s","未计余量流量 / kg/s")],[],[("Integrated dose / µSv","累计剂量 / µSv")]],
 "foodsafety":[[],[("Integrated Fref / min","累计 Fref / 分钟"),("Required Fref / min","目标 Fref / 分钟")],[],[]],
 "civil":[[("First HI / m","首站视线高 HI / m"),("Corrected first RL / m","首站校正高程 / m"),("Closure error / m","有符号闭合差 / m")],[("Support reaction / kN","支反力 / kN"),("Maximum moment / kN·m","最大弯矩 / kN·m")],[("Stress / MPa","最大应力 / MPa"),("Deflection / mm","最大挠度 / mm")],[("Wet fine aggregate / kg","湿细骨料 / kg"),("Wet coarse aggregate / kg","湿粗骨料 / kg"),("Added water / kg","应加水量 / kg")]],
}
books_zh={
 "telecom":[
  "光预算：Pr[dBm]=Pt[dBm]−光纤损耗[dB]−连接／熔接损耗[dB]−衰减器[dB]。光纤损耗＝长度[km]×衰减[dB/km]。余量＝Pr−接收灵敏度，单位 dB。本题要求 Pr≥−26＋3 dBm，且 Pr≤−13 dBm，防止接收过载。先选发射模块与衰减器，再计算对应 Pr 和余量。容差为 1% 或 0.05 dB，取较大值。",
  "SNR 的线性功率比＝10^(SNRdB/10)。香农容量 C=B log₂(1＋线性 SNR) 是理论上限。净速率＝Rs×log₂(M)×编码率×0.9；题面滚降下，占用带宽＝1.25Rs。设备门限：QPSK≥6 dB、16-QAM≥14 dB。需求以题面为准，范围为 3–9 Mbit/s；带宽≤5 MHz，净速率≤0.8C。B 用 MHz、Rs 用 Msym/s 时，所得速率单位为 Mbit/s。",
  "彩色参考导频 A–D 标识各 QPSK 符号，避免象限旋转歧义。通过相位、增益倍数、I/Q 直流修正，让实心点对准同色参考方框。EVM＝误差矢量均方根／参考矢量均方根，本图参考 RMS 为 1。设备先作相位和乘法增益处理，再叠加直流偏移。点击滑条或拖动，也可选择调节器后按方向键；Shift 为十倍步进。EVM≤3.5% 持续 800 ms 后验证。",
  "可用中心频率：900、905、910、915 MHz。每条连线表示两站至少相隔 10 MHz，即频道编号之差≥2。所有站点都要分配频道。可将频率卡片拖到站点，或点击对应 C1–C4。红线代表当前干扰冲突。本题调制与频道门限均为题面设备数据。",
 ],
 "nuclear":[
  "压水堆将一次回路热量经蒸汽发生器传给独立二次回路，两路流体不混合。识别反应堆热源、换热器与最终冷却池热阱。题面简化中子平衡：keff＝产生数／损失数；小于 1 为次临界，等于 1 为临界，大于 1 为超临界。本题从已停堆状态开始；次临界并不意味着没有衰变余热，仍须持续冷却。",
  "按题面教学曲线 P(t)=P0/(1＋t/60)^0.2 MW，在指定分钟求余热。该曲线是本题数据。排热 Q[kW]=流量[kg/s]×cp[kJ/(kg·K)]×ΔT[K]。求流量前将 MW 乘 1000 化为 kW。填写余热与未计余量流量，再选择额定流量≥需求×1.10 的最小泵。计算容差为 1%。",
  "A/B 使用不同电源母线与不同热阱，两路阀门开启。每路独立移除题面余热的 110%–125%，不超过已选泵额定流量。温度模型 Cth·dT/dt=P余热−Q冷却，Cth=10,000 kJ/K。测试共 6 秒：双路正常、A 失效、B 失效，各 2 秒；温度保持 80–95 °C。改变配置会重新开始测试。仪表和限值属于本题教学模型。",
  "题面为外照射点源近似：r 米处剂量率＝1 米剂量率／r²，再乘屏蔽透射系数。三段作业占总时间的 50%、30%、20%。累计剂量[µSv]＝Σ剂量率[µSv/h]×每段分钟数／60。至少完成规定作业时间，且满足题面剂量预算。根据各路线距离、屏蔽与时间计算；不能将探测器计数直接当剂量率。",
 ],
 "foodsafety":[
  "根据题面工艺和已验证措施分配：原料病原体→热处理 CCP；加工后金属→末端金检 CCP；含乳换线→清洁验证 GHP；清洁剂残留→进货／冲洗 GHP。GHP 建立一般卫生条件。是否为 CCP 要结合具体工艺和后续已验证控制步骤，不能只因存在危害就全部设为 CCP。拖动危害卡到措施槽，或使用卡片按钮。",
  "Dref 是指定产品和微生物在 Tref 温度下减少一个数量级的时间；目标 Fref＝目标 log 降低数×Dref。z 为 D 改变十倍对应的温差。对题面每个恒温矩形段：ΔF=分钟数×10^((T冷点−Tref)/z)，六段相加。前四段各 0.5 分钟，末两段各占保温时间一半。加热器比冷点高 4 °C，不能替代产品冷点。要求 F≥目标、F≤3 倍目标、冷点平台≤82 °C。",
  "生鸡肉和原料区刀具进入原料区；已包装即食食品进入即食区；含乳原料进入过敏原区。选择已验证清洁和专用工具流向。本题清洁验证报告仅确认已验证方法的过敏原残留合格；单纯冲洗或加热不能替代该验证。热处理后仍要防止再次污染。可拖动卡片到分区，或点击原料／即食／过敏原按钮。",
  "异常原料批影响所有连接的成品批，混批也受影响。按题面方案依次判断：不可逆化学残留→报废；异常原料的后代→暂扣；没有前两种问题的热处理偏差→已验证方案返工；其余完整记录→放行。检查全部连线和偏差标记。热返工不能解决化学残留或原料批不明风险。本题 D/z 与工艺限值只适用于所给产品、微生物和验证数据。",
 ],
 "civil":[
  "视线高 HI=已知高程 RL＋后视 BS；待测高程 RL=HI−前视 FS。逐站累计三段后，闭合差＝测得终点高程−已知终点高程。本题三段等长，每段校正 −闭合差／3，故首站校正高程＝首站原始高程−闭合差／3。输入首站 HI、首站校正 RL、有符号闭合差，单位 m。高程容差 0.001 m；闭合差容差 0.0005 m。选择负闭合差等分校正。",
  "本题为均布荷载简支梁：q[kN/m]、L[m]。每端支反力 R=qL/2 kN；跨中最大弯矩 M=qL²/8 kN·m。剪力图左端＋R、右端−R，跨中为零；正弯矩图为两端归零的抛物线。调节三个滑条或拖动图的两端剪力点和跨中弯矩点，再输入 R、M。方向键每步 0.5，Shift 每步 5。容差为 1% 或 0.1 单位。",
  "线弹性 Euler–Bernoulli 梁：σ=M/W。先将 kN·m 乘 1000 化为 N·m，再将 Pa 除 10⁶ 化为 MPa。δ=5qL⁴/(384EI)，其中 q 用 N/m、L 用 m、E 用 Pa、I 用 m⁴；所得 m 乘 1000 化为 mm。选择给定截面，满足应力≤160 MPa、挠度≤L/250、质量≤180 kg/m。E/I/W 与验收限值为题面数据，强度合格不代表挠度合格。",
  "总含水率 mc、吸水率 a 都以烘干 OD 质量为分母，计算时用小数。MOD=MSSD/(1＋a)；湿骨料=MOD(1＋mc)；游离水=MOD(mc−a)=湿质量−SSD 质量。应加水量=胶凝材料×0.45−Σ游离水。低于 SSD 时游离水为负，需要补足吸水。填写湿细／粗骨料与加水量，再选强度≥30 MPa、养护≥7 天的题面报告。水胶比不能直接证明混凝土强度或结构验收合格。",
 ]}
options={
 "telecom":{0:["−1","2","5"],1:["0","3","6","9"],2:["QPSK","16-QAM"],3:["1/2","3/4"],4:["1","2","3","4"]},
 "nuclear":{0:[("Source","热源"),("Exchanger","换热"),("Sink","热阱")],3:[("Mixed","混合"),("Separate","隔离")],4:[("Subcritical","次临界"),("Critical","临界"),("Supercritical","超临界")],6:["1","2"],10:[("Closed","关闭"),("Open","开启")],12:["1","2","3"],13:["1.0","0.5","0.2"]},
 "foodsafety":{0:[("GHP intake","进货GHP"),("Heat CCP","热CCP"),("Metal CCP","金检CCP"),("Allergen GHP","清洁GHP")],4:[("Raw","原料"),("RTE","即食"),("Allergen","过敏原")],8:[("Rinse","冲洗"),("Validated clean","验证清洁"),("Heat","加热")],9:[("Shared","共用"),("Dedicated","专用"),("Unverified","未核验")],10:[("Release","放行"),("Hold","暂扣"),("Reject","报废"),("Rework","返工")]},
 "civil":{0:[("No correction","不校正"),("−error / 3 per leg","每段−闭合差／3"),("+error / 3 per leg","每段＋闭合差／3")]},
}
en_add={};zh_add={}
def add(prefix,key,en,cn):
    en_add[prefix+key]=en;zh_add[prefix+key]=cn
def strings(block):
    return [json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"',block)]
for key,fallback in re.findall(r'common\("([^"\\]+)","([^"\\]+)"',source):
    add("task.goosetools.profession.",key,fallback,common_zh[key])
for key,fallback in [("check","Check"),("test","Test")]:add("task.goosetools.profession.",key,fallback,common_zh[key])
for key,fallback in re.findall(r'^\s*([A-Z_]+)\("([^"\\]+)"\)',feedback_source,re.M):
    key=key.lower();add("task.goosetools.profession.","feedback."+key,fallback,feedback_zh[key])
methods={"telecom":"telecom","nuclear":"nuclear","foodsafety":"food","civil":"civil"}
names={"telecom":"TELECOM","nuclear":"NUCLEAR","foodsafety":"FOODSAFETY","civil":"CIVIL"}
en_titles={"telecom":"Restore telecom link","nuclear":"Decay heat and radiation protection","foodsafety":"Food safety and batch release","civil":"Bridge checks and construction quality"}
for game,method in methods.items():
    prefix="task.goosetools."+game+"."
    body=source.split("private void "+method+"(",1)[1].split("private void ",1)[0]
    for key,fallback in re.findall(r'domain\("([^"\\]+)","([^"\\]+)"',body):add(prefix,key,fallback,zh[game][key])
    add(prefix,"title",en_titles[game],zh[game]["title"])
    for function,category in [("stages","stage"),("books","book")]:
        section=source.split("private String[] "+function+"()",1)[1].split("default->",1)[0]
        branch=section.split("case "+names[game]+"->new String[]{",1)[1].split("};",1)[0]
        values=strings(branch)
        # The next switch branch is excluded by the first closing array.
        for i,fallback in enumerate(values):add(prefix,category+str(i),fallback,(stages_zh if category=="stage" else books_zh)[game][i])
    for stage,values in enumerate(fields[game]):
        for i,(en,cn) in enumerate(values):add(prefix,f"field{stage}_{i}",en,cn)
    for slot,values in options[game].items():
        slots=[slot]
        if game=="nuclear" and slot==0:slots=[0,1,2]
        if game=="nuclear" and slot==6:slots=[6,7,8,9]
        if game=="nuclear" and slot==10:slots=[10,11]
        if game=="foodsafety" and slot==0:slots=list(range(4))
        if game=="foodsafety" and slot==4:slots=list(range(4,8))
        if game=="foodsafety" and slot==10:slots=list(range(10,16))
        for s in slots:
            for i,v in enumerate(values):
                en,cn=v if isinstance(v,tuple) else (v,v);add(prefix,f"option{s}_{i}",en,cn)
dynamic={
 "telecom":{},
 "nuclear":{"component":["Reactor","Steam generator","Cooling pool"],"flow":["Flow A / kg/s","Flow B / kg/s"]},
 "foodsafety":{"hazard":["Raw-material pathogen","Metal fragments after processing","Milk allergen at changeover","Cleaning-agent residue"],"control":["GHP intake / rinse","CCP heat treatment","CCP metal detection","GHP verified cleaning"],"zone":["RAW","READY-TO-EAT","ALLERGEN"],"tool":["Raw chicken","Packed ready food","Milk ingredient","Raw-area knife"]},
 "civil":{"aggregate":["Fine aggregate","Coarse aggregate"]},
}
for game,groups in dynamic.items():
    for group,values in groups.items():
        for i,en in enumerate(values):key=group+str(i);add("task.goosetools."+game+".",key,en,zh[game][key])
for i,en in enumerate(["Thermal deviation; rework validated","Chemical residue; irreversible"],1):add("task.goosetools.foodsafety.","deviation"+str(i),en,zh["foodsafety"]["deviation"+str(i)])
for lang,additions in [("en_us",en_add),("zh_cn",zh_add)]:
    path=root/"src/main/resources/assets/goosetools/lang"/(lang+".json")
    before=path.read_text(encoding="utf-8");old=json.loads(before)
    pending={k:v for k,v in additions.items() if k not in old}
    if pending:
        fragment=json.dumps(pending,ensure_ascii=False,indent=2)[1:-1].strip("\n")
        path.write_text(before.rstrip()[:-1].rstrip()+",\n"+fragment+"\n}\n",encoding="utf-8")
    assert all(json.loads(path.read_text(encoding="utf-8"))[k]==v for k,v in old.items())
    print(lang,"appended",len(pending),"keys")
