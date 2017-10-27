# Gitlet
A portable version-control system with local git functionality (commit, branch, merge, reset, etc.). 
Requirements: Java

Usage: To initialize a gitlet version control system into any directory, copy the "gitlet" folder into the directory. Then in
terminal of choice, execute the following command:
```
java gitlet.Main init
```
Then, go about your day with any local git commands by executing:
```
java gitlet.Main "command"
```
Compatible commands: init, add, commit, rm, log, global-log, find, status, branch, rm-branch, reset, merge.
