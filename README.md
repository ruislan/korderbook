# KOrderBook

A simple order match engine in Java and Kotlin

* JDK: 17 (Kotlin 1.9.10 hasn't supported JDK 21 yet, so we use JDK 17)
* Kotlin: 1.9.10
* Gradle: 8.4

## Run Locally

import into Jetbrains IDEA, Run test.

## Benchmark

Macbook Pro 16, i9 2.3G 8 cores, 16 GB 2667 MHz DDR4.

Jmh 1.3.7

```kotlin
warmupIterations = 1
threads = 100
```

| Benchmark                                                | Mode  | Cnt |    Score    | Units |    Error     |
|:---------------------------------------------------------|:-----:|:---:|:-----------:|:-----:|:------------:|
| c.r.k.java.OrderBookJavaPerformance.placeLimitOrders     | thrpt |  5  | 1412397.568 | ops/s | ± 205503.128 |
| c.r.k.kotlin.OrderBookKotlinPerformance.placeLimitOrders | thrpt |  5  | 1564446.464 | ops/s | ± 367446.901 |

```kotlin
warmupIterations = 1
threads = 200
```

| Benchmark                                                | Mode  | Cnt |    Score    |    Error     | Units |
|:---------------------------------------------------------|:-----:|:---:|:-----------:|:------------:|:-----:|
| c.r.k.java.OrderBookJavaPerformance.placeLimitOrders     | thrpt |  5  | 1300184.609 | ± 218893.053 | ops/s |
| c.r.k.kotlin.OrderBookKotlinPerformance.placeLimitOrders | thrpt |  5  | 1519423.199 | ± 211588.375 | ops/s |

