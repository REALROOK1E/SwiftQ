# SwiftQ 🚀

**A Next-Generation Message Queue System**

[![Java](https://img.shields.io/badge/Java-8+-blue.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-green.svg)](https://maven.apache.org/)
[![Netty](https://img.shields.io/badge/Netty-4.1+-orange.svg)](https://netty.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

SwiftQ is a high-performance, enterprise-grade message queue system built with modern Java technologies. It features intelligent state management, multi-dimensional indexing, dynamic routing, and asynchronous communication capabilities.

## 🌟 Key Features

### 🔄 **Intelligent State Machine**
- Complete message lifecycle management with 7 states
- Event-driven state transitions
- Automatic retry and failure handling
- State validation and consistency

### 🔍 **Multi-Dimensional Indexing**
- Topic-based indexing for fast categorization
- Tag-based indexing for flexible labeling
- Priority-based indexing for queue management
- Time-based indexing for temporal queries
- Complex query support with multiple conditions

### 🚦 **Dynamic Routing Engine**
- Rule-driven message routing
- Priority-based rule matching
- Tag and topic-based routing rules
- Extensible rule system
- Thread-safe rule management

### 🎛️ **Message Center Orchestrator**
- Centralized component coordination
- Event-driven architecture
- Statistics collection and monitoring
- Scheduled cleanup tasks
- Lifecycle management

### ⚡ **High-Performance Async Communication**
- Netty-based async I/O
- Producer/Consumer pattern
- JSON message serialization
- Auto-reconnection and fault tolerance
- Backpressure control

## 🏗️ Architecture

```
SwiftQ/
├── swiftq-common/     # Common components and message models
├── swiftq-core/       # Core functionality (State Machine, Index, Router)
├── swiftq-broker/     # Message broker server
└── swiftq-client/     # Client SDK
```

### Technology Stack
- **Java 8** - Core development language
- **Maven 3.9.11** - Build management
- **Netty 4.1.92** - Async network communication
- **Jackson 2.15.2** - JSON serialization
- **Lombok** - Code simplification
- **SLF4J** - Logging framework

## 🚀 Quick Start

### Prerequisites
- Java 8 or higher
- Maven 3.6+

### Building the Project
```bash
mvn clean compile
```

### Running Basic Demo
```bash
java -cp "swiftq-core/target/classes;swiftq-common/target/classes" com.swiftq.core.BasicDemo
```

### Starting the Broker
```bash
java -cp target/classes com.swiftq.broker.SwiftQBroker
```

### Producer Example
```java
SwiftQProducer producer = new SwiftQProducer("localhost", 8080);
producer.sendMessage("ORDER", "New order data");
```

### Consumer Example
```java
SwiftQConsumer consumer = new SwiftQConsumer("localhost", 8080);
consumer.setMessageHandler(message -> {
    System.out.println("Received: " + message.getBody());
});
```

## 📖 Core Concepts

### Message Model
```java
public class Message {
    private String id;                    // Unique identifier
    private String topic;                 // Message topic
    private MsgState state;               // Current state
    private Map<String, String> tags;     // Flexible tagging system
    private int priority;                 // Message priority (1-10)
    private long timestamp;               // Creation timestamp
    private int retryCount;               // Retry attempts
    private int maxRetries;               // Maximum retries
}
```

### Message States
```
INIT → SENDING → SENT → CONFIRMED ✅
  ↓       ↓        ↓
FAILED → RETRYING → DEAD ❌
```

### State Machine Usage
```java
// Create message and state machine
Message message = new Message("TEST", "Hello World", System.currentTimeMillis());
MessageStateMachine stateMachine = new MessageStateMachine(message);

// Handle events to transition states
stateMachine.handleEvent(StateEvent.SEND);     // INIT → SENDING
stateMachine.handleEvent(StateEvent.CONFIRM);  // SENDING → CONFIRMED
```

### Multi-Dimensional Indexing
```java
MessageMultiIndex index = new MessageMultiIndex();

// Add messages to index
index.addMessage(message1);
index.addMessage(message2);

// Query by topic
MessageQuery topicQuery = new MessageQuery().withTopic("ORDER");
List<Message> orders = index.query(topicQuery);

// Complex query: topic + tag + priority
MessageQuery complexQuery = new MessageQuery()
    .withTopic("ORDER")
    .withTag("urgent")
    .withMinPriority(8);
List<Message> results = index.query(complexQuery);
```

### Dynamic Routing
```java
DynamicRouter router = new DynamicRouter();

// Add routing rules
router.addRule(new TagPriorityRule("urgent", "true", 10, 1));
router.addRule(new TopicRule("ORDER", "order-queue", 2));

// Route messages automatically
List<String> queues = router.routeMessage(message);
```

## 🎯 Use Cases

### E-commerce Order Processing
```java
Message orderMsg = new Message("ORDER", orderData, timestamp);
orderMsg.setTags(Map.of("urgent", "true", "payment", "completed"));
orderMsg.setPriority(10);
messageCenter.publishMessage(orderMsg);
```

### Real-time Notification System
```java
Message notifyMsg = new Message("NOTIFY", notification, timestamp);
notifyMsg.setTags(Map.of("channel", "email", "type", "urgent"));
List<String> queues = router.routeMessage(notifyMsg);
```

### Task Scheduling System
```java
MessageQuery highPriorityTasks = new MessageQuery()
    .withTopic("TASK")
    .withMinPriority(8)
    .withTag("ready");
List<Message> tasks = index.query(highPriorityTasks);
```

## 🔧 Performance Features

### Concurrent Safety
- `ConcurrentHashMap` - Thread-safe index storage
- `CopyOnWriteArrayList` - Optimized for read-heavy scenarios
- `ConcurrentSkipListMap` - Ordered range queries

### Memory Optimization
- Lazy initialization of index structures
- Weak reference caching mechanisms
- Scheduled cleanup of expired messages

## 📊 Project Status

### ✅ Implemented Features
- [x] Multi-module Maven architecture
- [x] Enhanced Message model with state management
- [x] Complete state machine implementation
- [x] Multi-dimensional indexing system
- [x] Dynamic routing engine
- [x] Message center orchestrator
- [x] Async Netty-based communication
- [x] Basic Producer/Consumer demo

### 🚧 In Progress
- [ ] Comprehensive unit tests
- [ ] Performance benchmarking
- [ ] Configuration management
- [ ] Monitoring and metrics

### 🔮 Planned Features
- [ ] Persistent storage support
- [ ] Cluster mode and load balancing
- [ ] Management console
- [ ] Multi-language client SDKs
- [ ] Cloud-native deployment
- [ ] Stream processing capabilities

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Netty](https://netty.io/) for high-performance networking
- Uses [Jackson](https://github.com/FasterXML/jackson) for JSON processing
- Powered by [Maven](https://maven.apache.org/) for build management

---

**SwiftQ** - *Delivering messages at the speed of thought* ⚡
