package com.zyc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Locale;

public class MyFunction {
    public static class hsl {
       public double h=0;
       public double s=100;
       public double l=50;
    }
    public static class rgb {
       public double r=0;
       public double g=0;
       public double b=0;
    }

    //region 网络判断 返回值 -1：没有网络  1：WIFI网络2：wap网络3：net网络
    public static int GetNetype(Context context) {
        int netType = -1;
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            if (networkInfo.getExtraInfo().toLowerCase(Locale.getDefault()).equals("cmnet")) {
                netType = 3;
            } else {
                netType = 2;
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = 1;
        }
        return netType;
    }
    //endregion

    public static double MinD(double inA  , double inB)
    {
        if (inA < inB)  return inA; else return inB;
    }
    public static double MaxD(double inA , double inB  )
    {
        if (inA > inB) return inA; else return inB;
    }
    public static int DoubletoInt(double x  )
    {
        return Integer.parseInt(new java.text.DecimalFormat("0").format(x));
    }

    public static rgb HSLtoRGB(double HSL_h,double HSL_s,double HSL_l)
    {
        //返回值：RGB颜色
        //r,g,b:0-255
        // HSL_h:0-360
        // HSL_s:0-100
        // HSL_l:0-100
        double R,G,B,temp1_R,temp1_G,temp1_B,temp2_R,temp2_G,temp2_B;
        double h,s,l;

        if(HSL_s>100)HSL_s=100;
        if(HSL_s<0)	 HSL_s=0;

        if(HSL_l>100)HSL_l=100;
        if(HSL_l<0)	 HSL_l=0;


        h=HSL_h;
        s=HSL_s/100;
        l=HSL_l/100;
        //System.out.print("HSLtoRGB:"+h+","+s+","+l+":");
        while (h < 0)
            h = h + 360;
        while (h > 360)
            h = h - 360;

        if (h < 120)
        {
            temp1_R = (120 - h) / 60;
            temp1_G = h / 60;
            temp1_B = 0;
        }
        else if (h < 240)
        {
            temp1_R = 0;
            temp1_G = (240 - h) / 60;
            temp1_B = (h - 120) / 60;
        }
        else
        {
            temp1_R = (h - 240) / 60;
            temp1_G = 0;
            temp1_B = (360 - h) / 60;
        }

        temp1_R = MinD(temp1_R, 1);
        temp1_G = MinD(temp1_G, 1);
        temp1_B = MinD(temp1_B, 1);

        temp2_R = 2 * s * temp1_R + (1 - s);
        temp2_G = 2 * s * temp1_G + (1 - s);
        temp2_B = 2 * s * temp1_B + (1 - s);

        if (l < 0.5)
        {
            R = l * temp2_R;
            G = l * temp2_G;
            B = l * temp2_B;
        }
        else
        {
            R = (1 - l) * temp2_R + 2 * l - 1;
            G = (1 - l) * temp2_G + 2 * l - 1;
            B = (1 - l) * temp2_B + 2 * l - 1;
        }


        rgb a=new rgb();
        a.r=(R*255);
        a.g=(G*255);
        a.b=(B*255);
        //System.out.println("HSLtoRGB:"+(a.r)+","+(a.g)+","+(a.b));
        return a;

    }
    public static hsl RGBtoHSL(double RGB_r,double RGB_g,double RGB_b) //
    {
        //返回值：HSL颜色
        //RGB_r,RGB_g,RGB_b:0-255
        // h:0-360
        // s:0-100
        // l:0-100
        double themin , themax, delta ;
        double c2_H,c2_S,c2_L;
        double r,g,b;

        r=(RGB_r)/255;
        g=(RGB_g)/255;
        b=(RGB_b)/255;
        //System.out.println("RGBtoHSL:"+r+","+g+","+b+" ");

        themin = MinD(r, MinD(g, b));
        themax = MaxD(r, MaxD(g, b));

        delta = themax - themin;
        c2_L = (themin + themax) / 2;
        c2_S = 0.0;

        if ((c2_L > 0) && (c2_L < 1))
        {
            if (c2_L < 0.5)
            {
                c2_S = delta / (2 * c2_L);
            }
            else
            {
                c2_S = delta / (2 - 2 * c2_L);
            }
        }

        c2_H = 0.0;

        if (delta > 0)
        {
            if ((themax == r) && (themax != g))
            {
                c2_H = c2_H + (g - b) / delta;
            }
            if ((themax == g) && (themax != b))
            {
                c2_H = c2_H + (2 + (b - r) / delta);
            }
            if ((themax == b) && (themax != r))
            {
                c2_H = c2_H + (4 + (r - g) / delta);
            }

            c2_H = c2_H * 60;
        }
        //System.out.println((c2_H)+","+(c2_S)+","+(c2_L));
        hsl c2=new hsl();
        c2.h=(c2_H);
        c2.s=(c2_S*100);
        c2.l=(c2_L*100);
        //System.out.println((c2.h)+","+(c2.s)+","+(c2.l));
        return c2;
    }
}
