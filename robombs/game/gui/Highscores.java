package robombs.game.gui;

import robombs.game.model.*;
import java.util.*;

/**
 * An extended table for displaying the current scores. Highscores is a stupid for it...anyway...:-)
 */
public class Highscores extends Table {

	List<String> names=new ArrayList<String>();

    /**
     * Create a new instance. A new instance will be created everytime that the score changes.
     * But it's a quite lightweight component, so this shouldn't be a problem.
     * @param hi the HighscoreTable, i.e. the "model" that holds the actual data.
     */
    public Highscores(HighscoreTable hi) {
        super("Highscores", hi.getLineCount()+1, 3, 10,30,320,300);
        this.setColumnSize(0,150);
        this.setColumnSize(1,60);
        this.setColumnSize(2,60);
        this.setRowSize(0,20);
        this.setCell(0,0,"Name");
        this.setCell(0,1,"Wins");
        this.setCell(0,2,"Loses");
        names.clear();
        for (int i=0; i<hi.getLineCount(); i++) {
            String player=hi.getPlayerName(i);
            int wins=hi.getWins(i);
            int lost=hi.getLoses(i);
            this.setRowSize(i+1, 16);
            this.setCell(i+1,0,player);
            this.setCell(i+1,1,Integer.valueOf(wins));
            this.setCell(i+1,2,Integer.valueOf(lost));
            names.add(hi.getPlayerName(i));
        }
        Collections.sort(names);
    }

    public List<String> getPlayerNames() {
    	return names;
    }
        
}
