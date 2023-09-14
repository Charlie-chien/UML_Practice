import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

abstract class mode implements MouseInputListener {
    protected Canvas canvas = Canvas.getInstance();   // Canvas is singleton

    public void mousePressed(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseDragged(MouseEvent e) {
    }
    public void mouseClicked(MouseEvent e) {
    }
    public void mouseMoved(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
}
class Objectmode extends mode{
    protected String objectname= null;
    private Store_point store = new Store_point();
    public Objectmode(String objType) {
        this.objectname = objType;
    }


    public void mousePressed(MouseEvent mouseEvent) {
        BasicObject basicObj = store.new_object(objectname, mouseEvent.getPoint());
        canvas.addprototype(basicObj);
        canvas.repaint();
    }
}
class LineMODE extends mode{
    private String lineType = null;
    private Store_point store = new Store_point();
    private Point startP= null;
    private List<prototype> prototypes;
    private int portindex1 = -1, portindex2 = -1;
    private prototype proto1 = null, proto2 = null;

    public LineMODE(String linetype){
        this.lineType = linetype;
    }

    public void mousePressed(MouseEvent e) {
        prototypes = canvas.getPrototypesList();
        startP = findConnectedObj(e.getPoint(), "first");//record the object
    }

    public void mouseDragged(MouseEvent e){
        if(startP!=null){
            LineObject line = store.new_line(lineType,startP,e.getPoint());
            canvas.tempLine = line;
            canvas.repaint();
        }
    }
    public void mouseReleased(MouseEvent e) {
        Point endP = null;
        if (startP != null) {
            endP = findConnectedObj(e.getPoint(), "second");
            if (endP != null) {
                LineObject line = store.new_line(lineType, startP, endP);
                canvas.addprototype(line);
                line.setPorts(proto1.getPort(portindex1), proto2.getPort(portindex2));
                proto1.getPort(portindex1).addLine(line);
                proto2.getPort(portindex2).addLine(line);
            }
            canvas.tempLine = null;
            canvas.repaint();
            startP = null;
        }
    }
    private Point findConnectedObj(Point p, String target) {
        for (int i = 0; i < prototypes.size(); i++) {
            prototype proto = prototypes.get(i);
            int portIndex;
            String judgeInside = proto.inside(p);
            if (judgeInside != null && judgeInside != "insideLine") {
                if(judgeInside == "insideGroup"){
                    proto = proto.getSelectedShape();
                    portIndex = Integer.parseInt(proto.inside(p));
                }
                else
                    portIndex = Integer.parseInt(judgeInside);

                /* if inside the basic object, get the location of relative port */
                if ("first".equals(target)) {
                    proto1 = proto;
                    portindex1 = portIndex;
                } else if ("second".equals(target)) {
                    proto2 = proto;
                    portindex2 = portIndex;
                }
                Point portLocation = new Point();
                portLocation.setLocation(proto.getPort(portIndex).getCenterX(), proto.getPort(portIndex).getCenterY());
                return portLocation;
            }

        }
        return null;
    }

}
class SelectMODE extends mode {
    private List<prototype> prototypes;
    private Point startP = null;
    private String judgeInside = null;
    private LineObject selectedLine = null;

    public void mousePressed(MouseEvent e) {
        startP = e.getPoint();
        prototypes = canvas.getPrototypesList();
        canvas.reset();

        for (int i = prototypes.size() - 1; i >= 0; i--) {
            prototype proto = prototypes.get(i);
            judgeInside = proto.inside(e.getPoint());
            if (judgeInside != null) {
                canvas.selectedObj = proto; //select object
                break;
            }
        }
        canvas.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        int moveX = e.getX() - startP.x;
        int moveY = e.getY() - startP.y;

        if (canvas.selectedObj != null) {
            if (judgeInside == "insideLine") { // select line
                selectedLine = (LineObject) canvas.selectedObj;
                selectedLine.resetStartEnd(e.getPoint());
            }
            else { // select object
                canvas.selectedObj.resetLocation(moveX, moveY);
            }
            startP.x = e.getX();
            startP.y = e.getY();
        }
        else { // select area
            if (e.getX() >= startP.x && e.getY()>=startP.y)
                canvas.SelectedArea.setBounds(startP.x, startP.y, Math.abs(moveX), Math.abs(moveY));
            else if(e.getX() <= startP.x && e.getY()<= startP.y)
                canvas.SelectedArea.setBounds(e.getX(), e.getY(), Math.abs(moveX), Math.abs(moveY));
            else if(e.getX() <= startP.x && e.getY()>=startP.y)
                canvas.SelectedArea.setBounds(e.getX(),startP.y,Math.abs(moveX), Math.abs(moveY));
            else if(e.getX()>= startP.x && e.getY()<=startP.y)
                canvas.SelectedArea.setBounds(startP.x,e.getY(),Math.abs(moveX), Math.abs(moveY));
        }
        canvas.repaint();
    }

    public void mouseReleased(MouseEvent e) {

        if (canvas.selectedObj != null) {

            if (judgeInside == "insideLine") {
                selectedLine = (LineObject) canvas.selectedObj;
                reconnectLine(e.getPoint());
            }
        } else {
            canvas.SelectedArea.setSize(Math.abs(e.getX() - startP.x), Math.abs(e.getY() - startP.y));
        }
        canvas.repaint();
    }

    private void reconnectLine(Point p) {
        for (int i = 0; i < prototypes.size(); i++) {
            prototype proto = prototypes.get(i);
            int portindex;
            String judgeInside = proto.inside(p);
            if (judgeInside != null && judgeInside != "insideLine") {
                if (judgeInside == "insideGroup") {
                    proto = proto.getSelectedShape();
                    portindex = Integer.parseInt(proto.inside(p));
                } else {
                    portindex = Integer.parseInt(judgeInside);
                }
                selectedLine.resetPort(proto.getPort(portindex), selectedLine);
                selectedLine.resetLocation();
            }
        }
    }
}

class Store_point {

    public BasicObject new_object(String objectname, Point p){
        if(objectname.equals("class")){
            return new ClassObject(p.x, p.y);
        }
        else if(objectname.equals("usecase")){
            return new UsecaseObject(p.x, p.y);
        }
        return null;
    }
    public LineObject new_line(String linename, Point startP,Point endP){
        if(linename.equals("association")){
            return new AssociationLine(startP.x,startP.y,endP.x,endP.y);
        }
        else if(linename.equals("generalization")){
            return new GeneralizationLine(startP.x,startP.y,endP.x,endP.y);
        }
        else if(linename.equals("composition")){
            return new CompositionLine(startP.x,startP.y,endP.x,endP.y);
        }
        return null;
    }
}
abstract class prototype {

    protected int x1, y1, x2, y2;
    public boolean group_selected = false;

    public abstract void draw(Graphics g);

    public int getX1(){
        return x1;
    }
    public int getY1(){
        return y1;
    }
    public int getX2(){
        return x2;
    }
    public int getY2(){
        return y2;
    }

    public void resetLocation(){}   // for Line
    public void resetLocation(int moveX, int moveY){}  // for Basic object and Group

    public void changeName(String name){}
    public void show(Graphics g){}
    public String inside(Point p){
        return null;
    }
    //     Basic object
    public Port getPort(int portIndex){
        return null;
    }

    // Group
    public void resetSelectedShape() {}
    public prototype getSelectedShape() {
        return null;
    }
}
class Port extends Rectangle {
    private List<LineObject> lines = new ArrayList<LineObject>();
    public void setPort(int center_x,int center_y, int offset){
        int x = center_x - offset;
        int y = center_y - offset;
        int w = offset * 2;
        int h = offset * 2;
        setBounds(x,y,w,h);
    }
    public void addLine(LineObject line) {
        lines.add(line);
    }

    public void removeLine(LineObject line) {
        lines.remove(line);
    }

    public void resetLines() {
        for(int i = 0; i < lines.size(); i++){
            LineObject line = lines.get(i);
            line.resetLocation();
        }
    }

}
abstract class BasicObject extends prototype {
    private int offset = 10;
    protected int width, height;
    protected String objectname = "null-object";
    protected Port[] ports = new Port[4];

    public abstract void draw(Graphics g);

    public Port getPort(int portIndex) {
        return ports[portIndex];
    }

    public void show(Graphics g) {
        for (int i = 0; i < ports.length; i++) {
            g.fillRect(ports[i].x, ports[i].y, ports[i].width, ports[i].height);
        }
    }

    public String inside(Point p) {
        Point center = new Point();
        center.x = (x1 + x2) / 2;
        center.y = (y1 + y2) / 2;
        Point[] points = {new Point(x1, y1), new Point(x2, y1), new Point(x2, y2), new Point(x1, y2)};

        for (int i = 0; i < points.length; i++) {
            Polygon t = new Polygon();
            // (0,1,center) (1,2,center) (2,3,center) (3,0,center)
            int secondIndex = ((i + 1) % 4);
            t.addPoint(points[i].x, points[i].y);
            t.addPoint(points[secondIndex].x, points[secondIndex].y);
            t.addPoint(center.x, center.y);

            if (t.contains(p)) {
                return Integer.toString(i);
            }
        }
        return null;
    }

    public void resetLocation(int moveX, int moveY) {
        int x1 = this.x1 + moveX;
        int y1 = this.y1 + moveY;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x1 + width;
        this.y2 = y1 + height;
        int[] xpoint = {(x1+x2)/2, x2 + offset, (x1+x2)/2, x1 - offset};
        int[] ypoint = {y1 - offset, (y1+y2)/2, y2+offset, (y1+y2)/2};

        for(int i = 0; i < ports.length; i++) {
            ports[i].setPort(xpoint[i], ypoint[i], offset);
            ports[i].resetLines();
        }
    }

    public void changeName(String name) {
        this.objectname = name;
    }

    protected void createPorts() {
        int[] xpoint = {(x1 + x2) / 2, x2 + offset, (x1 + x2) / 2, x1 - offset};
        int[] ypoint = {y1 - offset, (y1 + y2) / 2, y2 + offset, (y1 + y2) / 2};

        for (int i = 0; i < ports.length; i++) {
            Port port = new Port();
            port.setPort(xpoint[i], ypoint[i], offset);
            ports[i] = port;
        }
    }

}
abstract class LineObject extends prototype {

    protected Port[] ports = new Port[2];
    public abstract void draw(Graphics g);
    private String selectedFlag = null;

    public void setPorts(Port port_1, Port port_2) {
        this.ports[0] = port_1;
        this.ports[1] = port_2;
    }

    public void show(Graphics g) {
        g.setColor(new Color(150, 30, 135));
        this.draw(g);
        g.setColor(new Color(20,30,140));
    }

    public void resetLocation(){
        this.x1 = (int) ports[0].getCenterX();
        this.y1 = (int) ports[0].getCenterY();
        this.x2 = (int) ports[1].getCenterX();
        this.y2 = (int) ports[1].getCenterY();
    }

    public void resetStartEnd(Point p) {
        if(selectedFlag == "start"){
            this.x1 = p.x;
            this.y1 = p.y;
        }
        else if(selectedFlag == "end") {
            this.x2 = p.x;
            this.y2 = p.y;
        }
    }

    public String inside(Point p) {
        int tolerance = 10;
        if(distance(p) < tolerance) {
            double distToStart = Math.sqrt(Math.pow((p.x - x1),2) + Math.pow((p.y - y1), 2));
            double distToEnd = Math.sqrt(Math.pow((p.x - x2),2) + Math.pow((p.y - y2), 2));
            if(distToStart < distToEnd) {
                selectedFlag = "start";
            }
            else{
                selectedFlag = "end";
            }
            return "insideLine";
        }
        else
            return null;
    }

    public void resetPort(Port port, LineObject line) {
        port.addLine(line);
        if(selectedFlag == "start"){
            this.ports[0].removeLine(line);
            this.ports[0] = port;
        }
        else if(selectedFlag == "end"){
            this.ports[1].removeLine(line);
            this.ports[1] = port;
        }
    }

    private double distance(Point p) {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        return line.ptLineDist(p.getX(), p.getY());
    }
}
class ClassObject extends BasicObject {

    public ClassObject(int x, int y){
        this.width=100;
        this.height=130;
        this.x1 = x;
        this.x2 = x+width;
        this.y1 = y;
        this.y2 = y+height;
        createPorts();
    }

    public void draw(Graphics g) {
        g.drawRect(x1,y1,width,height);
        g.drawString(objectname,x1+25,y1+25);

    }
}
class UsecaseObject extends BasicObject{
    public UsecaseObject(int x,int y){
        this.width = 100;
        this.height = 70;
        this.x1 = x;
        this.x2 = x+width;
        this.y1 = y;
        this.y2 = y+height;
        createPorts();
    }
    @Override
    public void draw(Graphics g) {
        g.drawOval(x1,y1,width,height);
        g.drawString(objectname,x1+20,y1+50);
    }
}

class AssociationLine extends LineObject {

    public AssociationLine(int x1,int y1,int x2, int y2){
        this.x1=x1;
        this.y1=y1;
        this.x2=x2;
        this.y2=y2;
    }

    @Override
    public void draw(Graphics g) {
        g.drawLine(x1,y1,x2,y2);
    }
}
class CompositionLine extends LineObject{
    public CompositionLine(int x1,int y1, int x2, int y2){
        this.x1=x1;
        this.y1=y1;
        this.x2=x2;
        this.y2=y2;
    }
    public void draw(Graphics g) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - 10, xn = xm, ym = 10, yn = -10, x;
        double sin = dy/D, cos = dx/D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;
        double xq = (20*2/D)*x1 + ((D-20*2)/D)*x2;
        double yq = (20*2/D)*y1 + ((D-20*2)/D)*y2;
        g.drawLine(x1,y1,x2,y2);
        g.drawLine((int)xm,(int)ym,x2,y2);
        g.drawLine((int)xn,(int)yn,x2,y2);
        g.drawLine((int)xq,(int)yq,(int)xn,(int)yn);
        g.drawLine((int)xq,(int)yq,(int)xm,(int)ym);
    }
}
class GeneralizationLine extends LineObject {

    public GeneralizationLine(int x1,int y1,int x2,int y2){
        this.x1=x1;
        this.y1=y1;
        this.x2=x2;
        this.y2=y2;
    }

    public void draw(Graphics g) {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - 10, xn = xm, ym = 10, yn = -10, x;
        double sin = dy/D, cos = dx/D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        g.drawLine(x1,y1,x2,y2);
        g.drawLine((int)xm,(int)ym,x2,y2);
        g.drawLine((int)xn,(int)yn,x2,y2);
    }
}
class Canvas extends JPanel {
    private static Canvas instance = null;

    private EventListener listener = null;
    protected mode currentMode = null;

    private List<prototype> prototypes = new ArrayList<prototype>();

    public prototype tempLine = null;
    public Rectangle SelectedArea = new Rectangle();
    public prototype selectedObj = null;
    private Canvas(){}

    public static Canvas getInstance() {
        if (instance == null){
            instance = new Canvas();
        }
        return instance;
    }
    public void setCurrentMode() {
        removeMouseListener((MouseListener) listener);
        removeMouseMotionListener((MouseMotionListener) listener);
        listener = currentMode;
        addMouseListener((MouseListener) listener);
        addMouseMotionListener((MouseMotionListener) listener);
    }

    public void reset() {
        if(selectedObj != null){
            selectedObj.resetSelectedShape();   // for selected shape inside the group
            selectedObj = null;
        }
        SelectedArea.setBounds(0, 0, 0, 0);
    }

    public void addprototype(prototype proto) {
        prototypes.add(proto);
    }

    public List<prototype> getPrototypesList(){
        return this.prototypes;
    }

    public void Groupproto(){}
    public void removeGroup(){}

    public void changeObjName(String name) {
        if(selectedObj != null){
            selectedObj.changeName(name);
            repaint();
        }
    }
    private boolean checkSelectedArea(prototype proto) {
        Point upperleft = new Point(proto.getX1(), proto.getY1());
        Point lowerright = new Point(proto.getX2(), proto.getY2());
        /* show ports of selected objects */
        if (SelectedArea.contains(upperleft) && SelectedArea.contains(lowerright)) {
            return true;
        }
        return false;
    }
    public void paint(Graphics g) {
        /* set canvas area */
        g.setColor(Color.lightGray);
        g.fillRect(0,0, 1200, 700);
        /* set painting color */
        g.setColor(new Color(30,150,50));
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(3));

        /* paint all shape objects */
        for (int i = prototypes.size() - 1; i >= 0; i--) {
            prototype proto = prototypes.get(i);
            proto.draw(g);
            proto.group_selected = false;
            if(!SelectedArea.isEmpty() && checkSelectedArea(proto)){
                proto.show(g);
                proto.group_selected = true;
            }
        }

        /* paint dragged line */
        if (tempLine != null) {
            tempLine.draw(g);
        }
        /* show ports when object is selected */
        if (this.selectedObj != null) {
            selectedObj.show(g);
        }
        /* paint area of group selection */
        if (!SelectedArea.isEmpty()) {
            int alpha = 85; // 33% transparent
            g.setColor(new Color(37, 148, 216, alpha));
            g.fillRect(SelectedArea.x, SelectedArea.y, SelectedArea.width, SelectedArea.height);
            g.setColor(new Color(237, 48, 16));
            g.drawRect(SelectedArea.x, SelectedArea.y, SelectedArea.width, SelectedArea.height);
        }
    }
}

class Menu extends JMenuBar {
    private Canvas canvas;
    public Menu(){
        canvas = Canvas.getInstance();

        JMenu menu;
        JMenuItem menuItem;

        menu = new JMenu("Edit");
        add(menu);
        menuItem = new JMenuItem("Change name!!");
        menu.add(menuItem);
        menuItem.addActionListener(new changenamelistener());
    }
    class changenamelistener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            final JFrame inputTextFrame = new JFrame("Change Object Name");
            inputTextFrame.setSize(400, 400);
            inputTextFrame.getContentPane().setLayout(new GridLayout(0, 1));

            JPanel panel = null;
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            final JTextField Text =  new JTextField("Object Name");
            panel.add(Text);
            inputTextFrame.getContentPane().add(panel);

            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

            JButton confirm = new JButton("OK");
            panel.add(confirm);

            JButton cancel = new JButton("Cancel");
            panel.add(cancel);

            inputTextFrame.getContentPane().add(panel);

            inputTextFrame.setLocationRelativeTo(null);
            inputTextFrame.setVisible(true);


            confirm.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canvas.changeObjName(Text.getText());
                    inputTextFrame.dispose();

                }
            });

            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    inputTextFrame.dispose();
                }
            });
        }
    }
}
class ToolButton extends JToolBar {
    private JButton holdBtn = null;
    private Canvas canvas;

    public ToolButton() {
        canvas = Canvas.getInstance();
        setLayout(new GridLayout(1, 6, 2, 2));
        this.setBackground(Color.CYAN);

        Button select = new Button("select",new SelectMODE());
        add(select);

        Button CLASS = new Button("class", new Objectmode("class"));
        add(CLASS);

        Button usecase = new Button("usecase", new Objectmode("usecase"));
        add(usecase);

        Button association = new Button("association",new LineMODE("association"));
        add(association);

        Button composition = new Button("composition",new LineMODE("composition"));
        add(composition);

        Button generalization = new Button("generalization",new LineMODE("generalization"));
        add(generalization);

    }
    private class Button extends JButton {
        mode toolMODE;
        public Button(String typename, mode toolMODE) {
            this.toolMODE = toolMODE;
            this.setText(typename);
            setToolTipText(typename);
            setFocusable(false);
            setBackground(Color.yellow);
            setBorderPainted(false);
            setRolloverEnabled(true);
            addActionListener(new toolListener());
        }
        class toolListener implements ActionListener {

            public void actionPerformed(ActionEvent actionEvent) {
                if (holdBtn != null)
                    holdBtn.setBackground(Color.yellow);
                holdBtn = (JButton) actionEvent.getSource();
                holdBtn.setBackground(Color.black);
                canvas.currentMode = toolMODE;
                canvas.setCurrentMode();
                canvas.reset();
                canvas.repaint();

            }
        }
    }
}

public class Demo extends JFrame{
    private Canvas canvas;
    private ToolButton toolButton;
    private Menu menu;
    public Demo(){
        canvas = Canvas.getInstance();
        toolButton = new ToolButton();
        menu = new Menu();
        toolButton.setFloatable(false);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(menu,BorderLayout.NORTH);
        getContentPane().add(canvas,BorderLayout.CENTER);
        getContentPane().add(toolButton,BorderLayout.SOUTH);

    }
    public static void main(String [] args){
        Demo window = new Demo();
        window.setTitle("UML");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setSize(1200,700);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
