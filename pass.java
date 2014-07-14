import java.awt.*;
import javax.swing.*;
public class pass extends JFrame
{
	JPanel mp = new JPanel();
	public static void  main(String[] args){
		pass ps = new pass();
	}
	public pass() {
		this.setTitle("powerd by honway");
		this.setSize(300,400);
		this.add(mp);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
	}
}
