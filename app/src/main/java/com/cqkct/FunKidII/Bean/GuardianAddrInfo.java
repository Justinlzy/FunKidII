package com.cqkct.FunKidII.Bean;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/3/20.
 */

public class GuardianAddrInfo implements Serializable {
    private static final long serialVersionUID = -4731784790906236224L;

    public String name;
    public String address;
    public double lat;
    public double lng;
    public int radius;
}
