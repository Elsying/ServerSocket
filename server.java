package serversocket;

import net.sf.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于简单的socket连接聊天，不推送消息，不判断对方socket是否 在线
 */

public class server {

	// 端口号
	private static final int PORT = 12345;
	// socket HashMap
	private HashMap<String, Socket> mList = new HashMap<String, Socket>();
	// 消息HashMap
	private List<Message> mMsgList = new ArrayList<Message>();
	private ServerSocket server = null;
	private ExecutorService myExecutorService = null;
	public static final String bm = "utf-8"; // 全局定义，以适应系统其他部分
	public Message message = new Message();

	public static void main(String[] args) throws IOException {
		new server();

	}

	public server() {
		try {

			server = new ServerSocket(PORT);
			// 创建一个可缓存线程池，如果线程池长度超过处理需要，可灵活回收空闲线程，若无可回收，则新建线程
			myExecutorService = Executors.newCachedThreadPool();
			InetAddress address = InetAddress.getLocalHost();
			String ip = address.getHostAddress();
			System.err.println("服务端运行中,服务端ip地址: " + ip + "...\n");
			Socket client = null;
			while (true) {
				client = server.accept();

				myExecutorService.execute(new Service(client));

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class Service implements Runnable {
		private Socket socket;
		private BufferedReader in = null;
		private String msg = "";

		public Service(Socket socket) {
			this.socket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
				msg = "用户" + this.socket.getInetAddress() + "连接成功";
				PrintWriter p = new PrintWriter(
						new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")), true);
				p.println("SUCCESS");
				System.out.println(msg);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void run() {
			try {
				while (true) {
					// 读取客户端发送过来的消息
					if ((msg = in.readLine()) != null) {
						message = readJson(msg);
						msg = "ID为：" + message.getMyID() + "发给" + message.getToID() + " 说: " + message.getMsg();
						mMsgList.add(message);// 消息添加到hashmap；
						mList.put(message.getMyID(), socket);// 标识socket添加到hashmap
						System.out.println(msg);
						// mList.remove(socket);
						// in.close();
						// socket.close();
					}
					if (mMsgList.size() > 0) {
						Iterator iter = mList.entrySet().iterator();
						while (iter.hasNext()) {
							Map.Entry entry = (Map.Entry) iter.next();
							for (int i = 0; i < mMsgList.size(); i++) {
								if (entry.getKey().equals(mMsgList.get(i).getToID())) {
									// if(!mList.get(entry.getKey()).isConnected()){
									// iter.remove();
									// }
									try {
										socket.sendUrgentData(0);
									} catch (IOException e) {
										iter.remove();
										break;
									}
									sendmsg(message.getToID(), message.getMsg());// 发送消息给指定的socket
									System.out.println("发送成功");
									mMsgList.remove(i);
									break;
								}

							}

						}
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		// 发送消息
		public void sendmsg(String key, String message) {
			// System.out.println(msg);

			Socket mSocket = mList.get(key);
			PrintWriter pout = null;
			try {
				pout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream(), "UTF-8")),
						true);
				pout.println(message);
				pout.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public Message readJson(String json) {
			JSONObject jsonObject = new JSONObject(json);
			message.setToID(jsonObject.getString("to"));
			message.setMsg(jsonObject.getString("msg"));
			message.setMyID(jsonObject.getString("MyID"));
			return message;

		}
	}
}
