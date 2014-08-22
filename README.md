## usage

* sbt -mem 4096 "run direct  10000000"
* sbt -mem 4096 "run forward 10000000"

## image

```
Direct                                          
                                                
            +-----------+                       
     Up     |           |                       
  +------>  |  Counter  |                       
            |           |                       
            +-----------+                       
                                                
                                                
Forward                                         
                                                
            +-----------+          +-----------+
     Up     |           |    Up    |           |
  +------>  |   Root    | +------> |  Counter  |
            |           |          |           |
            +-----------+          +-----------+
           (simply forward)
```

## benchmarks

* CPU:i7-980(3.3GHz), MEM:12GB
* send 10M messages to (direct|forward) actor

* direct: 5.1sec
* forward: 11.8sec
