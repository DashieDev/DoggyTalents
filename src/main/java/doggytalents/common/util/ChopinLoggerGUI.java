package doggytalents.common.util;

import java.awt.Font;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JLabel;

import com.google.common.collect.Maps;

import doggytalents.common.entity.DogEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;


public class ChopinLoggerGUI {

    public static ChopinLoggerGUI onlineGUI;

    private Map<String, Watcher<?>> namedWatcherMap = Maps.newConcurrentMap();
    private List<JLabel> labels = new ArrayList<>(4); 
    private boolean client;
    private World level;

    public static void openGUI(World world){
        
        onlineGUI = new ChopinLoggerGUI(world);
        onlineGUI.show();
    }

    private JFrame mainFrame;


    private ChopinLoggerGUI(World world) {
        this.client = world.isClientSide;
        this.level = world;
        this.prepare();
    }

    private void prepare() {
        this.mainFrame = new JFrame("ChopinLogger GUI");
        this.mainFrame.setSize(500, 700);

        this.mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.addWatchersAndLabels();

    }
    public void show() {
        this.mainFrame.setVisible(true);
    }

    public JFrame getMainFrame() {
        return this.mainFrame;
    }

    public boolean isDisposed() {
        return !this.mainFrame.isDisplayable();
    }

    public void addWatcher(String name, Watcher<?> watcher) {
        this.namedWatcherMap.computeIfAbsent(name, n -> watcher);
        this.addWatcherLabelToGUI(watcher);
    }

    public void addWatchersAndLabels() {
        JLabel label = new JLabel();
        label.setFont(new Font("Consolas", Font.PLAIN, 25 ));
        this.addWatcher("DogDeltaMovement", new Watcher<DogEntity>(
            label, d -> {
                Vector3d v = d.getDeltaMovement();
                return d.getName().getString() + " delta movement : " + v.toString();   
            }   
        ));
    }

    public void addWatcherLabelToGUI(Watcher<?> watcher) {
        this.mainFrame.add(watcher.getView()); 
    }

    public Watcher<?> getWatcher(String name) {
        return this.namedWatcherMap.get(name); 
    }

    public class Watcher<T> {
        private T watchee;
        private @Nonnull JLabel view;
        private @Nonnull printStringProvider<T> provider;

        public Watcher (JLabel view, printStringProvider<T> p)  {
            this.view = view;
            this.provider = p; 
        }

        public void setWatchee(T d) {
            this.watchee = d;
        }

        public T getWatchee() { return this.watchee; }
        public void updateValue() { 
            if (this.watchee == null) this.view.setText("null");
            this.view.setText(this.provider.get(this.watchee));
        }

        public JLabel getView() {
            return this.view;
        }

        
    }

    public interface printStringProvider<T> {
        public String get(T x);
    }

    public static void allowSwingGUI() {
        System.setProperty("java.awt.headless", "false");
    }
    

}

