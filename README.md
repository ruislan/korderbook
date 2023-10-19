# KOrderBook

A simple order match engine in Java and Kotlin

* JDK: 21
* Kotlin: 1.9.20-RC
* Gradle: 8.4

## Run Locally

import into Jetbrains IDEA, Run test.

## Benchmark

### MacBook Pro

MacBook Pro 16, i9 2.3G 4 cores, 16 GB 2667 MHz DDR4.

Jmh 1.3.7

```kotlin
warmupIterations = 1
threads = 100
```

| Benchmark                                          | Mode  | Cnt |    Score    | Units |    Error     |
|:---------------------------------------------------|:-----:|:---:|:-----------:|:-----:|:------------:|
| java.OrderBookJavaPerformance.placeLimitOrders     | thrpt |  5  | 1412397.568 | ops/s | ± 205503.128 |
| kotlin.OrderBookKotlinPerformance.placeLimitOrders | thrpt |  5  | 1564446.464 | ops/s | ± 367446.901 |

```kotlin
warmupIterations = 1
threads = 200
```

| Benchmark                                               | Mode  | Cnt |    Score    |    Error     | Units |
|:--------------------------------------------------------|:-----:|:---:|:-----------:|:------------:|:-----:|
| java.OrderBookJavaPerformance.placeLimitOrders          | thrpt |  5  | 1300184.609 | ± 218893.053 | ops/s |
| kotlin.OrderBookKotlinPerformance.placeLimitOrders      | thrpt |  5  | 1519423.199 | ± 211588.375 | ops/s |

### PC

Windows 11, Inter i7 8700k 3.7G 6 cores, 32G 3000 MHz DDR4

```kotlin
warmupIterations = 1
threads = 100
```
| Benchmark                                          | Mode  | Cnt |    Score    | Units |    Error     |
|:---------------------------------------------------|:-----:|:---:|:-----------:|:-----:|:------------:|
| java.OrderBookJavaPerformance.placeLimitOrders     | thrpt |  5  | 2383541.749 | ops/s | ± 163721.243 |
| kotlin.OrderBookKotlinPerformance.placeLimitOrders | thrpt |  5  | 2683735.289 | ops/s | ± 127344.966 |

```kotlin
warmupIterations = 1
threads = 200
```
| Benchmark                                          | Mode  | Cnt |    Score    | Units |    Error     |
|:---------------------------------------------------|:-----:|:---:|:-----------:|:-----:|:------------:|
| java.OrderBookJavaPerformance.placeLimitOrders     | thrpt |  5  | 2293407.721 | ops/s | ± 100826.909 |
| kotlin.OrderBookKotlinPerformance.placeLimitOrders | thrpt |  5  | 2508938.276 | ops/s | ± 228688.160 |
