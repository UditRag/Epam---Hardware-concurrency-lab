# Usage (PowerShell, from the project root): .\scripts\run.ps1   (needs a JDK 17+)
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem src\main\java\coherence\*.java).FullName
java -cp out coherence.TraceRunner
java -cp out coherence.TransitionTableDump
java -cp out coherence.InvariantCheck
