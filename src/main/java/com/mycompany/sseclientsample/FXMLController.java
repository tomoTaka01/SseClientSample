package com.mycompany.sseclientsample;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.util.Duration;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;

public class FXMLController implements Initializable {
    
    @FXML private Spinner<Integer> taskCnt;
    @FXML private Spinner<Integer> taskInterval;
    @FXML private Label label1;
    @FXML private Label label2;
    @FXML private Label label3;
    @FXML private Label label4;
    @FXML private Label label5;
    @FXML private Button startBtn1;
    @FXML private Button startBtn2;
    @FXML private Button closeBtn;
    
    EventSource eventSource;
    private Label[] labels;
    private TranslateTransition[] transitions;
    private BooleanProperty disableProperty;
    private BooleanProperty listeningProperty;
    
    /**
     * リスエスと送信（同期）ボタン処理.
     * 
     * @param event 
     */
    @FXML
    private void requestAction1(ActionEvent event) {
        listeningProperty.setValue(Boolean.TRUE);
        disableProperty.setValue(Boolean.FALSE);
        Arrays.stream(labels).forEach(l -> l.setText(null));
        int taskCntVal = taskCnt.getValue();
        int taskIntervalVal = taskInterval.getValue();
        String url = "http://localhost:8080/event/" 
                + taskCntVal + "/" + taskIntervalVal;
        Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
        WebTarget target = client.target(url);
        EventInput eventInput = target.request().get(EventInput.class);
        while (!eventInput.isClosed()){
            InboundEvent inboundEvent = eventInput.read();
            if (inboundEvent == null){
                break;
            }
            int id = Integer.parseInt(inboundEvent.getId());
            Label label = labels[id];
            String data = inboundEvent.readData(String.class);
            label.setText(data);                
            TranslateTransition t = transitions[id];
            new Timeline(new KeyFrame(Duration.millis(500), e-> t.play())).play();
        }
    }
    /**
     * リスエスと送信（非同期）ボタン処理.
     * 
     * @param event 
     */
    @FXML
    private void requestAction2(ActionEvent event){
        listeningProperty.setValue(Boolean.TRUE);
        disableProperty.setValue(Boolean.FALSE);
        int taskCntVal = taskCnt.getValue();
        int taskIntervalVal = taskInterval.getValue();
        String url = "http://localhost:8080/event/" 
                + taskCntVal + "/" + taskIntervalVal;
        Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
        WebTarget target = client.target(url);
        eventSource = EventSource.target(target).build();
        eventSource.register(inboundEvent ->{
            String data = inboundEvent.readData(String.class);
            int id = Integer.parseInt(inboundEvent.getId());
            Label label = labels[id];
            Platform.runLater(()->{
                label.setText(data);
                TranslateTransition t = new TranslateTransition(Duration.millis(300), label);
                t.setFromX(0);
                t.setToX(-200);
                t.play();
            });
        }, "message-client");
        eventSource.open();
    }
    
    /**
     * 接続終了
     */
    public void closeConnection(){
        if (eventSource != null){
            eventSource.close();
        }
        listeningProperty.setValue(Boolean.FALSE);
        disableProperty.setValue(Boolean.TRUE);
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labels = new Label[] {label1, label2, label3, label4, label5};
        Arrays.stream(labels).forEach(l -> l.setText(null));
        transitions = new TranslateTransition[]{
            new TranslateTransition(Duration.millis(500), label1)
           ,new TranslateTransition(Duration.millis(500), label2)
           ,new TranslateTransition(Duration.millis(500), label3)
           ,new TranslateTransition(Duration.millis(500), label4)
           ,new TranslateTransition(Duration.millis(500), label5)
        };
        Arrays.stream(transitions).forEach(t -> {
            t.setFromX(0);
            t.setToX(-200);
        });
        // Buttonの使用制御
        listeningProperty = new SimpleBooleanProperty(false);
        startBtn1.disableProperty().bind(listeningProperty);
        startBtn2.disableProperty().bind(listeningProperty);
        disableProperty = new SimpleBooleanProperty(true);        
        closeBtn.disableProperty().bind(disableProperty);
    }    
}
