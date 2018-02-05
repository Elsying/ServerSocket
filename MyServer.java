package serversocket;

import net.sf.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于的socket连接聊天，开启一个线程推送消息，对方掉线连上就会收到消息
 */

public class MyServer {
	private boolean isStartServer;
	private ServerSocket mServer;
	private ExecutorService myExecutorService = null;
	// 端口号
	private static final int PORT = 12345;
	/**
	 * 消息队列，用于保存SocketServer接收来自于客户机（手机端）的消息
	 */
	private List<SocketMessage> mMsgList = new CopyOnWriteArrayList<SocketMessage>();
	/**
	 * 线程队列，用于接收消息。每个客户机拥有一个线程，每个线程只接收发送给自己的消息
	 */
	private List<SocketThread> mThreadList = new CopyOnWriteArrayList<SocketThread>();

	/**
	 * 开启SocketServer
	 */
//	public static void main(String[] args) {
//		MyServer server = new MyServer();
//		server.startSocket();
//	}

	private void startSocket() {
		try {
			isStartServer = true;
			mServer = new ServerSocket(PORT);// 创建一个ServerSocket
			// 创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程
			myExecutorService = Executors.newCachedThreadPool();
			System.out.println("启动server,端口：" + PORT);
			Socket socket = null;
			// String socketID = null;//
			// Android（SocketClient）客户机的唯一标志，每个socketID表示一个Android客户机
			// 开启发送消息线程
			startSendMessageThread();
			while (isStartServer) {
				// accept()方法是一个阻塞的方法，调用该方法后，
				// 该线程会一直阻塞，直到有新的客户机加入，代码才会继续往下走
				socket = mServer.accept();
				// 有新的客户机加入后，则创建一个新的SocketThread线程对象
				SocketThread st = new SocketThread(socket);
				mThreadList.add(st);
				myExecutorService.execute(st);
				// 将该线程添加到线程队列
				;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 判断是否断开连接，断开返回true,没有返回false
	 *
	 */
	public Boolean isServerClose(Socket socket) {
		try {
			socket.sendUrgentData(0);// 发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
			return false;
		} catch (Exception se) {
			return true;
		}
	}

	/**
	 * 开启推送消息线程，如果mMsgList中有SocketMessage，则把该消息推送到Android客户机
	 */
	public void startSendMessageThread() {

		new Thread() {
			@Override
			public void run() {
				super.run();
				try {
					/*
					 * 如果isStartServer=true，则说明SocketServer已启动，
					 * 用一个循环来检测消息队列中是否有消息，如果有，则推送消息到相应的客户机
					 */

					while (isStartServer) {
						boolean sendme = false;
						// 判断消息队列中的长度是否大于0，大于0则说明消息队列不为空
						//System.out.println("开启推送:" + mMsgList.size());
						//Thread.sleep(1000);
						if (mMsgList.size() > 0) {
							// 读取消息队列中的第一个消息
							// SocketMessage from = mMsgList.get(0);
							// System.out.println("有消息：" + from.msg);
							for (SocketMessage from : mMsgList) {
								for (SocketThread to : mThreadList) {
									if (to.socketID.equals(from.to)) {
										if (isServerClose(to.socket)) {
											System.out.println("socketID：" + to.socketID + "断开连接,推送消息失败");
											mThreadList.remove(to);
											break;
										} else {
											PrintWriter writer = to.p;
											// JSONObject json = new
											// JSONObject();
											// json.put("from", from.from);
											// json.put("msg", from.msg);
											// writer写进json中的字符串数据，末尾记得加换行符："\n"，否则在客户机端无法识别
											// 因为BufferedReader.readLine()方法是根据换行符来读取一行的
											writer.println(from.msg);
											// 调用flush()方法，刷新流缓冲，把消息推送到手机端
											writer.flush();
											System.out.println("推送消息成功：" + from.msg + ">> to socketID:" + from.to);
											sendme = true;
											Thread.sleep(200);
											break;
										}
									}
								}
								// 每推送一条消息成功之后，就要在消息队列中移除该消息
								if (sendme == true) {
									mMsgList.remove(from);
									break;
								}
							}
						}
					}
					Thread.sleep(200);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 定义一个SocketThread类，用于接收消息
	 *
	 */
	public class SocketThread implements Runnable {
		public String socketID;
		public Socket socket;// Socket用于获取输入流、输出流
		public BufferedWriter writer;// BufferedWriter 用于推送消息
		public BufferedReader reader;// BufferedReader 用于接收消息
		public boolean close = false; // 关闭连接标志位，true表示关闭，false表示连接
		public PrintWriter p ;

		public SocketThread(Socket socket) {
			// socketID = count;
			this.socket = socket;
			try {
				p = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
				// 发送连接成功信息
				p.println("SUCCESS");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void run() {
			try {
				// 初始化BufferedReader
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf-8"));
				// 初始化BufferedWriter
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "utf-8"));

				// 初始化输出
				// 如果isStartServer=true，则说明SocketServer已经启动，
				// 现在需要用一个循环来不断接收来自客户机的消息，并作其他处理
				while (isStartServer) {
					// 判断是否断开
					close = isServerClose(socket);
					if (close) {
						System.out.println("socketID：" + socketID + "断开连接");
						break;
					}
					if (reader.ready()) {
						/*
						 * 读取一行字符串，读取的内容来自于客户机 reader.readLine()方法是一个阻塞方法，
						 * 从调用这个方法开始，该线程会一直处于阻塞状态， 直到接收到新的消息，代码才会往下走
						 */

						String data = reader.readLine();
						// 讲data作为json对象的内容，创建一个json对象
						JSONObject json = new JSONObject(data);
						// 创建一个SocketMessage对象，用于接收json中的数据
						SocketMessage msg = new SocketMessage();
						msg.from = json.getString("MyID");
						msg.to = json.getString("to");
						msg.msg = json.getString("msg");
						socketID = msg.from;
						if(!msg.to.equals("233")){
							mMsgList.add(msg);
						}
						System.out.println("新增一台客户机，socketID：" + socketID);
						System.out.println("收到一条消息：" + mMsgList.size() + json.getString("msg") + " >>>> to socketID:"
								+ json.getString("to"));
						// 睡眠100ms，每100ms检测一次是否有接收到消息
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
