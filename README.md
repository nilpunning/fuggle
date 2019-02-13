## Requirements
### Leiningen
```
lein upgrade

$ lein -version
Leiningen 2.5.3 on Java 1.8.0_25 Java HotSpot(TM) 64-Bit Server VM
```

### Docker
```
$ docker -v
Docker version 17.03.1-ce, build c6d412e
```

### Docker Compose
```
$ docker-compose --version                                        │
docker-compose version 1.23.2, build unknown        
```

## Operations
Deployments.
1. Local developement
2. Local uberjar to make sure uberjar is good before roll
3. Prod uberjar on AWS
    - See REPL deploy.clj functions 
    - Upload docker image to repo
    - Updates Cloudformation stack to point to new tag
    - Cloudformation automatically rolls containers
4.  Prod uberjar on single machine
    - Increment project version
    - uberjar-build.sh docker image
    - prod-compose.sh up -d

### Local Development with lein
1.  Start services
```
docker-compose rm -fv postgres # only when you want a fresh db
docker-compose build # only when necessary Dockerfile changed
docker-compose up -d
```
2.  Start Clojurescript REPL
```
./figheel.sh
```
Or if you want to test without figwheel (which depends on websockets), which don't work so well in old browsers (ex. IE).  First comment out anything to do with fighweel in cljs/fuggle.dev.  Second, instead of running figwheel, run:
```
docker-compose exec fuggle_dev lein cljsbuild auto
```

### Local uberjar
```
./uberjar-compose.sh rm -fv postgres # only when you want a fresh db
./uberjar-build.sh # only when necessary Dockerfile changed
./uberjar-compose.sh up
```

### Clojure REPL
REPL should work with all three deployment scenarios.  Prod will require SSH tunnel, see SSH section below.
1. Run -> Edit Configurations
1. `+` -> Clojure REPL -> Remote
1. Connect to server Host: localhost, Port: 5555
1. Run -> Run
1. `(fuggle.repl/go)`

### SSH
```
To ssh:
ssh -i fuggle.pem ec2-user@<ip>

To ssh tunnel into prod repl:
ssh -i fuggle.pem -NT -L 5555:localhost:5555 ec2-user@<ip>
```

### Mapping dependencies
```
lein ns-dep-graph -platform :clj -name clj
lein ns-dep-graph -platform :cljs -name cljs
```

### Debugging dependency conflicts
```
lein pom
mvn dependency:tree -Dverbose=true 
```

## How to
Dump db
```
pg_dumpall -U fuggleuser -h postgres  > db.out
```
Reload db
```
psql -U postgres -f db.out postgres
```
REPL inside container
```
lein repl :connect 0.0.0.0:5555
```