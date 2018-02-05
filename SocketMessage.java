package serversocket;

public class SocketMessage {
	public String to;// socketID，指发送给谁
	public String from;// socketID，指谁发送过来的
	public String msg;// 消息内容
	public String time;// 接收时间
	// public SocketThread thread;//socketThread下面有介绍
}
