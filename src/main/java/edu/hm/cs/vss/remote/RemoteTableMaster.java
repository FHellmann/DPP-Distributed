package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Philosopher;
import edu.hm.cs.vss.TableMaster;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemoteTableMaster implements TableMaster {
    @Override
    public boolean isAllowedToTakeSeat(Philosopher philosopher) {
        return false;
    }
}
