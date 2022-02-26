# Simple Time Tracker

A simple command line time tracker written as a babashka script.

Prerequisite:
`Babashka` installed.

Usage:
Make the script executable (Optionally rename it to a simple name like `stt`).
Use the `-f` flag every time if you want to use a different data storage file. 
Data is stored in CSV format.

```bash
$ stt -h
  -f, --data-file File      ~/.simple-time-tracker-data.csv  The file to store all data.
  -s, --status                                               The running session.
  -S, --simple-status                                        For easy connection with other tools.
  -n, --new                                                  Create a new session.
  -N, --new-with-tags Tags                                   Create a new session with tags, divided by comma, surrounded by double quote. e.g. "tag1, tag2"
  -d, --drop                                                 Drop the current session.
  -c, --complete                                             Complete the current session.
  -p, --pause                                                Pause the current session.
  -r, --resume                                               Resume paused session,
  -t, --today                                                Get summary of today.
  -h, --help
```
