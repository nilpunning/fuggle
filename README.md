# Fuggle

A simple recipe web app created to explore isomorphic web app design in Clojure and Clojurescript.  The app will function with Javascript turned off in the browser, but will have improved capability when turned on.

## Why is this called Fuggle?
[Fuggle](https://en.wikipedia.org/wiki/List_of_hop_varieties#Fuggle) is a type of hops.

## Requirements
Leiningen
```
$ lein -version
Leiningen 2.7.1 on Java 1.8.0_202 OpenJDK 64-Bit Server VM
```

Docker
```
$ docker -v
Docker version 18.09.2-ce, build 62479626f2 
```

Docker Compose
```
$ docker-compose --version
docker-compose version 1.23.2, build unknown
```

### Quickstart
1.  Start using uberjar build
    ```
    ./uberjar-build.sh
    ./uberjar-compose.sh up
    ```
1.  Navigate to [http://localhost:5000](http://localhost:5000)
1.  Log in using these credentials:
    ```
    username: fake@email.com
    password: pw
    ```
    
## How to build the site
### Local uberjar
```
./uberjar-compose.sh rm -fv postgres  # only when you want a fresh db
./uberjar-build.sh                    # only when necessary Dockerfile changed
./uberjar-compose.sh up
```

### Local development
1.  Start services
    ```
    docker-compose rm -fv postgres  # only when you want a fresh db
    docker-compose build            # only when necessary Dockerfile changed
    docker-compose up -d
    ```
2.  Start Clojurescript REPL
    ```
    ./figheel.sh
    ```
    Or if you want to test without figwheel (which depends on websockets), which does not work so well in old browsers (ex. IE).  First comment out anything to do with fighweel in cljs/fuggle.dev.  Second, instead of running figwheel, run:
    ```
    docker-compose exec fuggle_dev lein cljsbuild auto
    ```

### Clojure REPL
REPL should work with both deployment scenarios.  For example with Cursive:
1. Run -> Edit Configurations
1. `+` -> Clojure REPL -> Remote
1. Connect to server Host: localhost, Port: 5555
1. Run -> Run
1. `(go)`

### Map dependencies
```
lein ns-dep-graph -platform :clj -name clj
lein ns-dep-graph -platform :cljs -name cljs
```

### Debug dependency conflicts
```
lein pom
mvn dependency:tree -Dverbose=true 
```

### Dump db
```
pg_dumpall -U fuggleuser -h postgres  > db.out
```

###Reload db
```
psql -U postgres -f db.out postgres
```

### Run a REPL inside the container
```
lein repl :connect 0.0.0.0:5555
```

Copyright 2016 David O'Meara