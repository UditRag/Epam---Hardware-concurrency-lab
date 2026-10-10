# Module 05 references

| Claim supported | Source | Location | Supports / does not establish |
|---|---|---|---|
| MESI state definitions | Intel 64 and IA-32 Architectures SDM, Vol. 3A (https://www.intel.com/content/www/us/en/developer/articles/technical/intel-sdm.html) | Cache Control Protocol, Table 11-4 (section number varies by edition: verify) | Intel MESI example; not universal, not MOESI |
| Purpose of Owned state | Intel, "Faster Core-to-Core Communications" (https://www.intel.com/content/www/us/en/developer/articles/technical/faster-core-to-core-communications.html) | Whole article | Explanatory, not a normative spec |
| ACE five-state model is a different protocol | Arm AMBA AXI and ACE Protocol Specification, IHI 0022H (https://documentation-service.arm.com/static/602a9df190ee6824a1e02b98) | Appendix D | Not MESI/MOESI |
| Java happens-before | JLS SE 21, Chapter 17 (https://docs.oracle.com/javase/specs/jls/se21/html/jls17.html) | 17.4.5 | Java ordering, not hardware states |

Links were carried over from the module handoff and not re-opened while building this package.
