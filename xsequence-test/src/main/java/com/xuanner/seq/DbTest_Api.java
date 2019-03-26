package com.xuanner.seq;

import com.xuanner.seq.sequence.Sequence;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by xuan on 2018/1/10.
 */
public class DbTest_Api extends BaseTest {

    private Sequence sequence;

    @Before
    public void setup() {
        sequence = getDbSequence();
    }

    @Test
    public void test() {
        test(sequence);
    }

}
