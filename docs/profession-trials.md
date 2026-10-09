# Advanced professional task trials

Open a trial with `/goosetools tasks open <players> <type>` and close with
`/goosetools tasks close <players>`. Self-trial types are:

| Type | Four stages |
| --- | --- |
| `telecom` | Optical link budget; modulation and throughput; QPSK IQ calibration; frequency graph |
| `nuclear` | PWR heat-flow diagram; supplied decay-heat curve; redundant cooling outage test; integrated dose planning |
| `foodsafety` | GHP/CCP controls; cold-point D/z/F treatment; zone/tool separation; mixed-lot trace and disposition |
| `civil` | Leveling closure; beam forces and diagrams; section stress/deflection; concrete moisture correction and reports |

The panel explains every unit, model assumption, equipment rating, error tolerance
and acceptance window. Its handbook provides four pages. Errors preserve completed
stages. Enter checks; Tab changes numeric focus; arrows adjust selected dials;
Shift adjusts ten steps. Sliders, channel/hazard/tool cards and beam diagram points
also support dragging. A cooling configuration must pass a six-second server
simulation; IQ calibration must remain stable for 800 ms. There is no short
question countdown; professional sessions expire after one hour.

The given decay curve, modulation thresholds, processing organism/product data,
radiation geometry and engineering acceptance limits are teaching question data.
Food disposition follows the displayed priority: irreversible chemical residues
rejected, failed-raw-lot descendants held, other validated thermal deviations
reworked, compliant records released. Beam checks use the specified uniform-load
linear model; aggregate moisture and absorption both use oven-dry mass.

The four task IDs append to existing enum IDs. Separate fixed-size profession
messages keep earlier task, electrical station and arcade formats unchanged.
The server validates the stage, session, sequence, rate, numerical range and
conditions; the client has no completion operation. Every professional state is
a protected non-pausing screen, including waiting, handbooks and results. Optional
InvMove is intercepted without changing settings or requiring it to be installed.

These remain command trials. They do not create map NPCs, navigation markers,
task assignments, achievements or normal map-task progress. Existing authoritative
completion, replacement and cancellation/cleanup events are reused.

Resource packs can override:

```text
assets/goosetools/textures/gui/tasks/professions/dial_knob.png
assets/goosetools/textures/gui/tasks/professions/link_terminal.png
```

Those textures reuse the established task device artwork. Vanilla items depict
food, tools and aggregates. Charts and plant/beam diagrams are drawn by the GUI.
Text catalogs append only `en_us` and `zh_cn`; older keys are preserved.

Rules are checked with independent calculations and full solutions for 1,000
seeds per profession. The test-only client source set exercises real packets,
rendered controls and all scales; see `src/professionRegressionTest/README.md`.

Model references used during implementation:

- [Cisco optical link budgets](https://www.cisco.com/c/en/us/td/docs/interfaces_modules/port_adapters/install_upgrade/atm/pa-a3_ATM_install_config/pa_a3/5117ovr.html)
- [Keysight capacity and receiver performance](https://www.keysight.com/blogs/en/tech/rfmw/2019/02/26/simplify-receiver-characteristic-and-performance-tests)
- [Keysight IQ impairments](https://www.keysight.com/blogs/en/tech/rfmw/2019/05/22/confronting-measurement-uncertainty-in-signal-generation-part-4-iq-impairments)
- [NRC PWR circuits](https://www.nrc.gov/education-regulatory-research/the-student-corner/the-pressurized-water-reactor-pwr)
- [NRC subcriticality](https://www.nrc.gov/education-regulatory-research/glossary/subcriticality)
- [NRC time, distance and shielding](https://www.nrc.gov/facilities-safety/radiation-protection/how-the-nrc-protects-you/minimize-your-exposure)
- [FAO GHP/HACCP toolbox](https://www.fao.org/good-hygiene-practices-haccp-toolbox/en)
- [FDA D/z/F definitions](https://www.fda.gov/inspections-compliance-enforcement-and-criminal-investigations/inspection-guides/sterilizing-symbols-d-z-f)
- [MIT beam bending and deflection](https://live.ocw.mit.edu/courses/1-050-solid-mechanics-fall-2004/f9a4d5764b9b0f51ffaacbdf69f7aed8_emech10_04.pdf)
- [FHWA concrete moisture/absorption corrections](https://highways.dot.gov/sites/fhwa.dot.gov/files/docs/federal-lands/materials/forms/13066/1638_v6.pdf)
