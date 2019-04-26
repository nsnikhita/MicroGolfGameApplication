package com.uic.snaram2.microgolf;

/**
 * Created by nikhi on 4/14/2018.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static android.graphics.Color.GREEN;
import static java.lang.Thread.sleep;

public class GameActivity extends AppCompatActivity {
    int winning_hole;
    ScrollView scrollview;
    View Images[];
    static final int NEAR_MISS = 1;
    static final int NEAR_GROUP = 2;
    static final int BIG_MISS = 3;
    Handler player1_handler,player2_handler;
    int player1_choice = 10;
    int player2_choice = 10;
    int t_choice = 10;
    static final Object Lock1 = new Object();
    HashMap<Integer,Boolean> hashmap = new HashMap<Integer, Boolean>();
    Thread player1_thread,player2_thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.game_activity, null);

        // Find the ScrollView
        scrollview = (ScrollView) v.findViewById(R.id.scrollView);

        // Create a LinearLayout element
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Add text
        Images = new View[50];
        for (int i = 0; i < 50; i++)
        {
            TextView tv = new TextView(this);
            tv.setText(i+" ");
            tv.setTextColor(Color.BLACK);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setX(600);
            tv.setBackgroundResource(R.drawable.circle);
            Images[i] = tv;
            layout.addView(tv);
        }

        scrollview.addView(layout);
        // Display the view
        setContentView(v);

        //declaring threads
        winning_hole = rangen(0,50);
        Images[winning_hole].setBackgroundResource(R.drawable.circle_green);
        player1_thread = new Thread(new Player1()) ;
        player1_thread.start();
        player2_thread = new Thread(new Player2()) ;
        player2_thread.start();
    }
    //random shot numbers within range
    public int rangen(int minimum,int maximum){
        Random r = new Random();
        int int1 = r.nextInt(maximum - minimum) + minimum;
        return(int1);
    }
    //checking if the chosen number falls with in the range
    public boolean decision1(int lrange,int rrange,int choice){
        if((choice>=lrange && choice<rrange) && (winning_hole>=lrange && winning_hole<rrange)){
            return  true;
        }
        else
            return false;
    }

    //uihandler
    private Handler uiHandler = new Handler() {
        public void handleMessage(Message msg) {
            synchronized (Lock1) {
                int what = msg.what;
                int Player_choice = msg.arg1;
                //checking if it is a chosen ball then set display
                if (player1_choice != 10 && what == 0){
                    Images[player1_choice].setBackgroundResource(R.drawable.circle);
                }
                if (player2_choice != 10 && what == 1){
                    Images[player2_choice].setBackgroundResource(R.drawable.circle);
                }
                // compare with the already exixting key in the hashmap to see if that hole is chosen by other player already
                if ((Player_choice == player1_choice) || (Player_choice == player2_choice)) {
                    if (what == 0) {
                        Toast.makeText(getBaseContext(), "!!CATASTROPHE!! , PLAYER2 WON", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(getBaseContext(), "!!CATASTROPHE!! , PLAYER1 WON", Toast.LENGTH_SHORT).show();
                    player1_thread.interrupt();
                    player2_thread.interrupt();
                    //handling the shot after interruptions to remove the remaining jobs from the queue
                    try {
                        Thread.sleep(100);
                    }catch (Exception e)
                    {

                    }
                    player1_handler.getLooper().quit();
                    player2_handler.getLooper().quit();
                    uiHandler.removeCallbacksAndMessages(null);
                    player1_handler.removeCallbacksAndMessages(null);
                    player2_handler.removeCallbacksAndMessages(null);
                    return;
                }
                //checking if the shot made by player is winning hole
                if (winning_hole == Player_choice) {
                    player1_thread.interrupt();
                    player2_thread.interrupt();
                    try {
                        Thread.sleep(100);
                    }catch (Exception e)
                    {

                    }
                    player1_handler.getLooper().quit();
                    player2_handler.getLooper().quit();
                    player1_handler.removeCallbacksAndMessages(null);
                    player2_handler.removeCallbacksAndMessages(null);

                    uiHandler.removeCallbacksAndMessages(null);
                    if (what == 0)
                        Toast.makeText(getBaseContext(), "PLAYER1 WON ", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getBaseContext(), "PLAYER2 won", Toast.LENGTH_SHORT).show();
                    return;
                }
                // if it is not catastrophe or winning shot do this
                int left_bound = (Player_choice / 10) * 10;
                int right_bound = left_bound + 10;
                boolean decision = decision1(left_bound, right_bound, Player_choice);
                // add the shot to the hashmap
                hashmap.put(Player_choice, true);
                t_choice = Player_choice;

                if (what == 0){
                    player1_choice=Player_choice;
                }
                if (what == 1){
                    player2_choice=Player_choice;
                }
                // setting color to the shot made by the first player and second player
                if (what == 0) {
                    Images[Player_choice].setBackgroundResource(R.drawable.circle_blue);
                    scrollview.smoothScrollTo(0,Images[Player_choice].getTop());

                }
                else{
                    Images[Player_choice].setBackgroundResource(R.drawable.circle_orange);
                    scrollview.smoothScrollTo(0,Images[Player_choice].getTop());

                }
                // checking if the shot made by player1 or player2 is near miss
                if (decision == true) {
                    Message message;
                    if (what == 0) {
                        Toast.makeText(getBaseContext(), "Player1 Near miss", Toast.LENGTH_SHORT).show();
                        message = player1_handler.obtainMessage();
                        message.what = NEAR_MISS;
                        message.arg1 = Player_choice;
                        player1_handler.sendMessage(message);

                    } else {
                        Toast.makeText(getBaseContext(), "Player2 Near miss", Toast.LENGTH_SHORT).show();
                        message = player2_handler.obtainMessage();
                        message.what = NEAR_MISS;
                        message.arg1 = Player_choice;
                        player2_handler.sendMessage(message);
                    }

                    return;
                }
                if (decision == false) {
                    if (left_bound == 0 || right_bound == 49) {
                        if (left_bound == 0)
                            right_bound = right_bound + 10;
                        if (right_bound == 49)
                            left_bound = left_bound - 10;
                    } else {
                        right_bound = right_bound +10;
                        left_bound = left_bound - 10;
                    }
                    // checking if the shot is neargroup
                    decision = decision1(left_bound, right_bound, Player_choice);
                    if (decision) {
                        Message message;
                        if (what == 0) {
                            Toast.makeText(getBaseContext(), "Player1 Near Group", Toast.LENGTH_SHORT).show();
                            message = player1_handler.obtainMessage();
                            message.what = NEAR_GROUP;
                            message.arg1 = Player_choice;
                            player1_handler.sendMessage(message);

                        } else {
                            Toast.makeText(getBaseContext(), "Player2 Near Group", Toast.LENGTH_SHORT).show();
                            message = player2_handler.obtainMessage();
                            message.what = NEAR_GROUP;
                            message.arg1 = Player_choice;
                            player2_handler.sendMessage(message);
                        }
                        return;  // checking if the shot is a big miss
                    } else {
                        Message message;
                        if (what == 0) {
                            Toast.makeText(getBaseContext(), "Player1 Big Miss", Toast.LENGTH_SHORT).show();
                            message = player1_handler.obtainMessage();
                            message.what = BIG_MISS;
                            message.arg1 = Player_choice;
                            player1_handler.sendMessage(message);

                        } else {
                            Toast.makeText(getBaseContext(), "Player2 Big Miss", Toast.LENGTH_SHORT).show();
                            message = player2_handler.obtainMessage();
                            message.what = BIG_MISS;
                            message.arg1 = Player_choice;
                            player2_handler.sendMessage(message);
                        }

                        return;
                    }
                }

            }
        }
    };


    public class Player1 implements Runnable {
        int val;
        HashMap<Integer,Boolean> player1_hmap =  new HashMap<Integer,Boolean>();
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                return;
            } else {
                Looper.prepare();
            // player1 handler
                player1_handler = new Handler() {
                    public void handleMessage(Message msg) {
                        int what = msg.what;
                        int left_bound = (msg.arg1 / 10) * 10;
                        int right_bound = left_bound + 9;
                        // responses to the UITHREAD if it is a near miss
                        switch (what) {
                            case NEAR_MISS:
                                int p1_choice = rangen(left_bound, right_bound);
                                //checking if that shot is already made by it
                                while (player1_hmap.containsKey(p1_choice)) {
                                    p1_choice = rangen(left_bound, right_bound);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player1_hmap.put(p1_choice, true);
                                Message mess1 = uiHandler.obtainMessage();
                                mess1.what = 0;
                                mess1.arg1 = p1_choice;
                                uiHandler.sendMessage(mess1);
                                break;
                         // response to the uithread if it is a near group
                            case NEAR_GROUP:
                                int l = left_bound;
                                int r = right_bound;
                                if (left_bound != 0 && right_bound != 49) {
                                    l = left_bound - 10;
                                    r = right_bound + 10;
                                } else if (left_bound == 0) {
                                    r = right_bound + 10;
                                    l = left_bound + 10;
                                } else if (right_bound == 49) {
                                    l = left_bound - 10;
                                    r = right_bound - 10;
                                }
                                int rand = rangen(l, r);
                                // checking if that shot is already made by it
                                while (player1_hmap.containsKey(rand)) {
                                    rand = rangen(l, r);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player1_hmap.put(rand, true);
                                Message mess2 = uiHandler.obtainMessage();
                                mess2.what = 0;
                                mess2.arg1 = rand;
                                uiHandler.sendMessage(mess2);
                                break;
                         // response to the uithread if it is a big miss
                            case BIG_MISS:

                                rand = rangen(0, 49);
                                // checking if that shot is already made by it
                                while (player1_hmap.containsKey(rand)) {
                                    rand = rangen(0, 49);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player1_hmap.put(rand, true);
                                Message mess3 = uiHandler.obtainMessage();
                                mess3.what = 0;
                                mess3.arg1 = rand;
                                uiHandler.sendMessage(mess3);
                                break;
                        }
                    }
                };
                // for the first shot pick randomly
                if (player1_hmap.isEmpty()) {
                    val = rangen(0, 50);
                    player1_hmap.put(val, true);
                    try {
                        sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message mess4 = uiHandler.obtainMessage();
                    mess4.what = 0;
                    mess4.arg1 = val;
                    uiHandler.sendMessage(mess4);
                } else
                    System.out.println("Player1");

                Looper.loop();
            }
        }
    }
    public class Player2 implements Runnable {
        HashMap<Integer,Boolean> player2_hmap =  new HashMap<Integer,Boolean>();
        public void run() {
            if(Thread.currentThread().isInterrupted()){
                return;
            }
            else{
                Looper.prepare();
                player2_handler = new Handler() {
                    public void handleMessage(Message msg) {
                        int what = msg.what;
                        int left_bound = (msg.arg1 / 10) * 10;
                        int right_bound = left_bound + 10;
                        switch (what) {
                            // response of player2 thread if it is a nearmiss
                            case NEAR_MISS:
                                int a = rangen(0,49);
                                // checking if this shot is already made by him
                                while(player2_hmap.containsKey(a)){
                                    a = rangen(0,49);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player2_hmap.put(a,true);
                                Message message1 = uiHandler.obtainMessage();
                                message1.what = 1;
                                message1.arg1 = a;
                                uiHandler.sendMessage(message1);
                                break;
                             // response of player2 to uithread if ir is a neargroup
                            case NEAR_GROUP:

                                int rd = rangen(0,49);
                                //checking if this shot is already made by him
                                while(player2_hmap.containsKey(rd)){
                                    rd = rangen(0,49);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player2_hmap.put(rd,true);
                                Message message2 = uiHandler.obtainMessage();
                                message2.what = 1;
                                message2.arg1 = rd;
                                uiHandler.sendMessage(message2);
                                break;
                                // response to uithread if it is a bigmiss
                            case BIG_MISS:
                                int  rand = rangen(0,49);
                                // checking if this shot is already made by him
                                while(player2_hmap.containsKey(rand)){
                                    rand = rangen(0,49);
                                }
                                try {
                                    sleep(4000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                player2_hmap.put(rand,true);
                                Message message3 = uiHandler.obtainMessage();
                                message3.what = 1;
                                message3.arg1 = rand;
                                uiHandler.sendMessage(message3);
                                break;
                        }
                    }
                };
                // for the first random shot
                if(player2_hmap.isEmpty()){
                    int  val = rangen(0,50);
                    player2_hmap.put(val,true);
                    try {
                        sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Message message4 = uiHandler.obtainMessage();
                    message4 .what = 1;
                    message4 .arg1 = val;
                    uiHandler.sendMessage(message4 );
                }
                else
                    System.out.println("Player2");
                Looper.loop();
            }
        }
    }
}


