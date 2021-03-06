# A Java Library for Distributed Communication

# [KitchenSync on Google+](https://plus.google.com/100262181000574738434)

# Quickstart

* Ivy dependency - &lt;dependency org="com.github.mrstampy" name="KitchenSync-core" rev="2.3.6"/&gt;
* [Example code](https://github.com/mrstampy/KitchenSync-core/tree/master/KitchenSync-core/test/com/github/mrstampy/kitchensync/test)

# Release 2.3.6 - September 3, 2014

* getChunk implementation was overengineered in [BufferedInputStreamStreamers](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/BufferedInputStreamStreamer.java), simplified.

# Release 2.3.5 - September 2, 2014

* Improvements in [ByteArrayStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/ByteArrayStreamer.java)

# Release 2.3.4 - August 31, 2014

* Added [ResettingLatch](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/util/ResettingLatch.java) utility class.
* [ChunkProcessor](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/header/ChunkProcessor.java) API change, streamer now supplied when determining header size

# Release 2.3.3 - August 30, 2014

* bugfix in [ByteArrayStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/ByteArrayStreamer.java), automatic end of message sending fixed.

# Release 2.3.2 - August 25, 2014

* Exposed throttling in [ByteArrayStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/ByteArrayStreamer.java).

# Release 2.3.1 - August 23, 2014

* Removed explicit header reference from Streamer interface
* Added [ChunkProcessor](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/header/ChunkProcessor.java) and [Footer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/footer/Footer.java) for transforming chunks and sending end of message messages, respectively
* Added a throttle for full throttle and acknowledgement messages to slow down throughput.
* Improvements to [ByteArrayStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/ByteArrayStreamer.java).
* Upgraded Netty to version 4.0.23.Final

# Release 2.2.3 - August 17, 2014

* default chunk size for streamers increased to 2048 bytes
* added [end of message](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/EndOfMessageRegister.java) functionality
* created [ByteArrayStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/ByteArrayStreamer.java) for streaming byte arrays, [tester](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/test/com/github/mrstampy/kitchensync/test/stream/ByteArrayStreamerTester.java) class to demonstrate usage
* bug fixes for [BufferedInputStreamStreamers](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/BufferedInputStreamStreamer.java) which don't finish on empty streams

# Release 2.2.2 - August 14, 2014

* bugfix, sent value not adjusted for header
* bugfix, unintended and unnecessary overrides in BufferedInputStreamStreamer
* better code organization

# Release 2.2.1 - August 12, 2014

* Two new functions added to the Streamer interface, [concurrent threads and header with sequence per chunk](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/Streamer.java)
* New [StreamerHeader](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/StreamerHeader.java) class for header specifics
* Increasing the number of concurrent threads greatly boosts performance, over 150 megabytes/second at full throttle and over 40 megabytes per second for acknowledgement messages

# Release 2.1 - August 10, 2014

* Added [Streamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/Streamer.java) architecture to stream arbitrarily large amounts of data
* Three modes of streaming - full throttle, chunks per second and ack required
* Two implementations: [BufferedInputStreamStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/BufferedInputStreamStreamer.java) and [FileStreamer](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/stream/FileStreamer.java)
* [Test class](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/test/com/github/mrstampy/kitchensync/test/stream/StreamerTester.java) to demonstrate usage

# Release 2.0 - August 2, 2014

* Initial release
* Core functionality extracted from [KitchenSync](https://github.com/mrstampy/KitchenSync) which now remains as a simple reference implementation
* Strong typing of abstract channels

# KitchenSync Architecture

KitchenSync-core is a Java Library for non-centralized network communication between separate processes using the [UDP](http://en.wikipedia.org/wiki/User_Datagram_Protocol) protocol.  Channels can be created as multicast channels which allow broadcasting of messages to all connected channels, port-specific channels or next-port-available channels and are intended to be easily created and destroyed as required. It is built on top of [Netty](http://netty.io) and is designed to be simple to understand and use while providing the ability to customise individual channels.  

Two interfaces - [KiSyChannel](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/KiSyChannel.java) and [KiSyMulticastChannel](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/KiSyMulticastChannel.java) - provide the API for network communication.  Three abstract implementations - [AbstractKiSyChannel](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/AbstractKiSyChannel.java), [AbstractPortSpecificKiSyChannel](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/AbstractPortSpecificKiSyChannel.java), and [AbstractKiSyMulticastChannel](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/AbstractKiSyMulticastChannel.java) - exist for ease of channel creation:

	public class ByteArrayChannel extends
			AbstractKiSyChannel<ByteArrayByteBufCreator, ByteArrayMessageInitializer, NioDatagramChannel> {
	
		@Override
		protected ByteArrayMessageInitializer initializer() {
			return new ByteArrayMessageInitializer();
		}
	
		@Override
		protected Class<NioDatagramChannel> getChannelClass() {
			return NioDatagramChannel.class;
		}
	
		@Override
		protected ByteArrayByteBufCreator initByteBufCreator() {
			return new ByteArrayByteBufCreator();
		}
	
	}

The [ChannelInitializer](http://netty.io/4.0/api/io/netty/channel/ChannelInitializer.html) is a Netty class which is used to initialise a [Bootstrap](http://netty.io/4.0/api/io/netty/bootstrap/Bootstrap.html) object for the channel and is ignored if the Bootstrap [already exists](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/DefaultChannelRegistry.java).  [Netty channel handlers](http://netty.io/4.0/api/io/netty/channel/ChannelHandler.html) are added to the channel's pipeline in the implementation of the ChannelInitializer to control the channel's behaviour such as using SSL for connections:

	@Override
	protected void initChannel(DatagramChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast(new SslHandler(context.createSSLEngine()));
		pipeline.addLast(new KiSyMessageHandler());
	}

KiSyChannels can send and receive one of two types of messages by default - [byte arrays](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/initializer/ByteArrayMessageInitializer.java) and [strings](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/netty/channel/initializer/StringMessageInitializer.java).

## Inbound and Outbound

KitchenSync-core adds to the Netty architecture - which provides the ability to add custom handlers to interface with applications - by providing [inbound](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/message/inbound/KiSyInboundMessageManager.java) and [outbound](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/message/outbound/KiSyOutboundMessageManager.java) message managers which are initialised on application startup to apply [inbound](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/message/inbound/KiSyInboundMesssageHandler.java) and [outbound](https://github.com/mrstampy/KitchenSync-core/blob/master/KitchenSync-core/src/com/github/mrstampy/kitchensync/message/outbound/KiSyOutboundMessageHandler.java) application specific KitchenSync handler implementations to messages.  This separates channel configuration (in the ChannelInitializer) from message processing such as logging of messages, persistence of messages, autonomous event triggering on message, etc.  The handlers' execution is ordered to allow sequential operations to take place.  Note that the handlers exist for preparation of messages for processing by the application and execution of any presend logic.  Any significant processing of the message should be done on a separate thread.  Strictly speaking only one implementation is necessary - inbound, to pull messages into the application.


