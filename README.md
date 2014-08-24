## usage

*   sbt -mem 4096 "run direct 100000"
*   sbt -mem 4096 "run lookup 100000"

## image

http://www.plantuml.com:80/plantuml/png/oymhIIrAIqnELGWjJYtoJSnBJ4yjuk92uYZefkWgk6GMfIQNE2QNv1TXoFdavsUN5a3akBYGL89axS3cGhXM2gN5gLn8oh4g0000

```
interface UserManager

UserManager -d- DirectActor
UserManager -d- LookupActor

DirectActor -d-> UserActor : use
LookupActor -d-> UserActor : use
```

## benchmarks

* CPU:i7-980(3.3GHz), MEM:12GB
* send 10M messages to (direct|forward) actor

* direct: 100K 5283 msec (100k times hash.get(key) ! msg)
* lookup: 100K 6025 msec (100k times actorSelection(path) ! msg)
