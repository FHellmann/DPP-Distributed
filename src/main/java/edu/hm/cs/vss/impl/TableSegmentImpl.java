package edu.hm.cs.vss.impl;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.TableSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Fabio Hellmann on 13.04.2016.
 */
public class TableSegmentImpl implements TableSegment {
    private final List<Chair> chairs = Collections.synchronizedList(new ArrayList<>());
}
