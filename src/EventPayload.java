
public class EventPayload {
	public static class Point {
		private int x;
		private int y;
		public void setX(int x) {
			this.x = x;
		}
		public int getX() {
			return x;
		}
		public void setY(int y) {
			this.y = y;
		}
		public int getY() {
			return y;
		}
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private int player_;
	private Point vertex_;
	public void setPlayer(int player_) {
		this.player_ = player_;
	}
	public int getPlayer() {
		return player_;
	}
	public void setVertex(Point vertex_) {
		this.vertex_ = vertex_;
	}
	public Point getVertex() {
		return vertex_;
	}
	
	public EventPayload(int player, Point vertex) {
		player_ = player;
		vertex_ = vertex;
	}
	
}
