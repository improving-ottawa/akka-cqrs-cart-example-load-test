# Cart service load test

Very simple load test project for driving load to the _**Shopping Cart Service**_ over grpc using [Gatling](https://gatling.io/) and [grpc-gatling](https://github.com/phiSgr/gatling-grpc) extension. 

## Running the load test locally

Please use the following instructions to run this load test on your local machine.

1.  Deploy `shopping-cart-service` locally:

    ```
    $> cd ~/akka-cqrs-cart-example-load-test/shopping-cart-service
    $> sbt clean compile
    $> sbt publishLocal
    $> sbt 'set run/javaOptions += "-Dconfig.resource=local1.conf"; run'
    ```

2. For load testing, launch the `gatling` test by execute following commands in a separate command-shell on your local machine:

    ```
    $> cd ~/akka-cqrs-cart-example-load-test/load-testing
    $> sbt clean compile
    $> sbt gatling:test
    ```
    Successful execution of this load will result in output **_similar_** to the following:
    ```
    [info] welcome to sbt 1.8.2 (Homebrew Java 19.0.2)
    [info] loading global plugins from /Users/.../.sbt/1.0/plugins
    [info] loading settings for project load-testing-build from plugins.sbt ...
    [info] loading project definition from /Users/.../akka-cqrs-cart-example-load-test/load-testing/project
    [info] loading settings for project loadTesting from build.sbt ...
    [info] set current project to shopping-cart-load-test (in build file:/Users/.../akka-cqrs-cart-example-load-test/load-testing/)
    [warn] sbt 0.13 shell syntax is deprecated; use slash syntax instead: Gatling / test
    [info] compiling 3 Scala sources to /Users/.../akka-cqrs-cart-example-load-test/load-testing/target/scala-2.13/test-classes ...
    Gatling 3.9.2 is available! (you're using 3.9.0)
    Using Target(127.0.0.1,8101) as target environment for load test
    Starting Shopping cart load test at 1 users per second
    Simulation com.lightbend.akka.samples.load.ShoppingCartServiceLoadTest started...
    
    ================================================================================
    2023-02-28 11:33:16                                           4s elapsed
    ---- Requests ------------------------------------------------------------------
    > Global                                                   (OK=470    KO=10    )
    > Add Cart Item                                            (OK=46     KO=10    )
    > Update Cart Item                                         (OK=423    KO=0     )
    > Checkout                                                 (OK=1      KO=0     )
    ---- Errors --------------------------------------------------------------------
    > grpcStatusCode.find.is(OK), but actually found INVALID_ARGUMEN     10 (100.0%)
    T
    
    ---- Fill a shopping cart and check out ----------------------------------------
    [##########################################################################]100%
    waiting: 0      / active: 0      / done: 1     
    ================================================================================
    Simulation com.lightbend.akka.samples.load.ShoppingCartServiceLoadTest completed in 4 seconds
    Parsing log file(s)...
    Parsing log file(s) done
    Generating reports...
    
    ================================================================================
    ---- Global Information --------------------------------------------------------
    > request count                                        480 (OK=470    KO=10    )
    > min response time                                      1 (OK=5      KO=1     )
    > max response time                                    273 (OK=273    KO=6     )
    > mean response time                                     8 (OK=8      KO=2     )
    > std deviation                                         12 (OK=12     KO=1     )
    > response time 50th percentile                          7 (OK=7      KO=2     )
    > response time 75th percentile                          8 (OK=8      KO=2     )
    > response time 95th percentile                         10 (OK=10     KO=5     )
    > response time 99th percentile                         13 (OK=13     KO=6     )
    > mean requests/sec                                     96 (OK=94     KO=2     )
    ---- Response Time Distribution ------------------------------------------------
    > t < 800 ms                                           470 ( 98%)
    > 800 ms <= t < 1200 ms                                  0 (  0%)
    > t ≥ 1200 ms                                            0 (  0%)
    > failed                                                10 (  2%)
    ---- Errors --------------------------------------------------------------------
    > grpcStatusCode.find.is(OK), but actually found INVALID_ARGUMEN     10 (100.0%)
    T
    ================================================================================
    Reports generated in 0s.
    Please open the following file: file:///Users/.../akka-cqrs-cart-example-load-test/load-testing/target/gatling/shoppingcartserviceloadtest-20230228193312467/index.html
    [info] Simulation ShoppingCartServiceLoadTest successful.
    [info] Simulation(s) execution ended.
    [success] Total time: 12 s, ....
    ```
    Please note that a small % of requests will fail at this time. For example, 10 (2%) out of 480 requests failed in the above load-test run.

## Manage request load in the test
   The request load in this test is controlled in two dimensions. These dimensions are 'no. of users' and 'time duration', and their values are currently controlled through the following two lines of code in `load-testing/src/test/scala/com/lightbend/akka/samples/load/ShoppingCartServiceLoadTest.scala`:

   ```
   private val users = System.getProperty("requestsPerSecond", "1").toInt  // # of users

   private val loadDuration: FiniteDuration = (1 * 60).seconds // test duration in seconds
   ```
   As shown above, by default the load test is run with one user and for one minute. However, you can control the load by changing the no. of users or the test duration or both.
   
   For example, the following shows how you can increase the test load by increasing the no of users to two and increasing the test duration to five minutes:
   ```
   private val users = System.getProperty("requestsPerSecond", "2").toInt  // # of users

   private val loadDuration: FiniteDuration = (5 * 60).seconds // test duration in seconds
   ```

   Please follow all the instructions mentioned in the **_'Running the load test locally'_** section above, starting from instruction #2, after making the required changes in the `ShoppingCartServiceLoadTest.scala`.
=======
```
$> sbt gatling:test
```

### Deploying to K8s

The approach is based on the high-level [guidelines here](https://gatling.io/docs/gatling/guides/scaling_out/#scaling-out-with-gatling-open-source)

#### Build docker image

```
$> sbt clean docker:publishLocal
```

#### Test docker image

The image will publish it's results to `/mnt/simulation-results/results-<pod-name>` where `<pod-name>` is sourced from environment variable `POD_NAME`.  The command below provides a value `xyz` as POD_NAME.  First create a local directory `result` to mount as a volume in the container, then try the image with this command:

```
$> docker run -it -v ${PWD}/results:/mnt/simulation-data -e POD_NAME=xyz shopping-cart-load-test/shopping-cart-load-test-driver:0.1.0-SNAPSHOT
```
