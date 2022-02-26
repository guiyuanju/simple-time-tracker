# Simple Time Tracker

An simple command line time tracker written as a babashka script.

Prerequisite:
`Babashka` installed.

Usage:
Make the script executable, use the `-f` flag to point to your desired data storage file. (Store in CSV format)

```bash
$ stt -h
  -f, --data-file File      /home/YOUR_USER_NAME/.simple-time-tracker-data.csv  The file to store all data.
  -s, --status                                                       The running session.
  -S, --simple-status                                                The running session simple information.
  -n, --new                                                          Create a new session.
  -N, --new-with-tags Tags                                           Create a new session with tags, divided by comma, surrounded by double quote. ("tag1, tag2")
  -d, --drop                                                         Drop the current session.
  -c, --complete                                                     Complete the current session.
  -p, --pause                                                        Pause the current session.
  -r, --resume                                                       Resume paused session,
  -t, --today                                                        Get summary of today.
  -h, --help
```
