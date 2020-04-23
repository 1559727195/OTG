package com.firstopenglproject.otg;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

import static com.firstopenglproject.otg.BytesHexStrTranslate.bytesToHexFun1;

public class MainActivity extends AppCompatActivity {


    private int usb_used;
    private PendingIntent mPermissionIntent;
    private int WRITEBUF_SIZE = 4;
    private TextView txt_show;


    public void connect_usb(View view) {
        UsbSerch(mPermissionIntent);
    }

    public void get_usb_value(View view) {
        common_send("AA0061000547545003E89F66FF", "获取温度值发送成功 : ", "获取温度值发送失败");
    }

    private void common_send(String cmd, String sucess, String fail) {
        String content = cmd;
        byte[] buffer = new byte[content.length() / 2];
        buffer = toBytes(content);
        if (epOut == null) {
            Toast.makeText(getApplicationContext(), "usb epout = null", Toast.LENGTH_LONG).show();
            return;
        } else {
            Toast.makeText(getApplicationContext(), "usb epout is ok", Toast.LENGTH_LONG).show();
        }
        int length = buffer.length;
        int timeout = 5000;
        int cnt = connection.bulkTransfer(epOut, buffer, length, timeout); //发送bulk 数据给下位机
        if (cnt >= 0) {
            //vib.vibrate(100);
            Toast.makeText(getApplicationContext(), sucess + cnt, Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(getApplicationContext(), fail, Toast.LENGTH_LONG).show();
    }


    /**
     * 将16进制字符串转换为byte[]
     *
     * @param str
     * @return
     */
    public static byte[] toBytes(String str) {
        if (str == null || str.trim().equals("")) {
            return new byte[0];
        }
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    public void test_cmd_comunnity(View view) {
        common_send("AA0062000145F6D0FF", "测试通讯命令发送成功 : ", "测试通讯命令发送失败");
    }

    public void test_cmd_reset(View view) {
        common_send("AA006300035253545DBCFF", "设置复位命令发送成功 : ", "设置复位命令发送失败");
    }

    public void test_cmd_get_version(View view) {
        common_send("AA00640003484656CD9BFF", "获取版本号命令发送成功 : ", "获取版本号命令发送失败");
    }

    public void test_cmd_search_default(View view) {
        common_send("AA0066000343445380EDFF", "查询故障代码命令发送成功 : ", "查询故障代码命令发送失败");
    }

    public void test_cmd_set_frenquce(View view) {
        common_send("AA0067043F733333175DFF", "设置物体发射率命令发送成功 : ", "设置物体发射率命令发送失败");
    }


    class usbMsgThread extends Thread { // usb消息 接收线程
        UsbEndpoint epIn;
        UsbDeviceConnection connection;
        private Handler msgHandler; // Handler

        public usbMsgThread(UsbDeviceConnection connection, UsbEndpoint epIn,
                            Handler msgHandler) { // 构造函数，获得mmInStream和msgHandler对象
            this.epIn = epIn;
            this.connection = connection;
            this.msgHandler = msgHandler;
        }

        public void run() {
            final byte[] InBuffer = new byte[16]; // 创建 缓冲区,1次传输 8个字节
            int length = InBuffer.length;
            int timeout = 5000;
            while (!Thread.interrupted()) {
                final int cnt = connection.bulkTransfer(epIn, InBuffer, length, timeout); // 接收bulk数据
                // Toast.makeText(getApplicationContext(), "红灯命令发送成功-收到数据 : " + cnt, Toast.LENGTH_LONG).show();
                if (cnt < 0) { // 没有接收到数据，则继续循环
                    continue;
                }
                Message msg = new Message(); // 定义一个消息,并填充数据
                msg.obj = InBuffer;
                msgHandler.sendMessage(msg); // 通过handler发送消息
            }
        }
    }


    UsbManager usbManager; // usb管理器对象
    UsbDevice mydevice; // usb设备对象
    UsbInterface intf; // usb接口对象
    UsbDeviceConnection connection; // usb设备连接对象
    UsbEndpoint epOut, epIn; // 输入、输出 端点 对象

    usbMsgThread usb_Msg_Thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_show = findViewById(R.id.txt_show);
        // 设置广播接收器
        mPermissionIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        UsbSerch(mPermissionIntent);
    }


    private void UsbSerch(final PendingIntent mPermissionIntent) {
        // debug("start search USB device.");
        String strMsg = new String();
        strMsg = "设备属性:";
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE); // 获取usb服务
        if (usbManager == null) {
            Toast.makeText(getApplicationContext(), "获取USB服务失败",
                    Toast.LENGTH_LONG).show();
            return;
        }
        // 搜索所有的usb设备
        HashMap<String, UsbDevice> deviceList = usbManager
                .getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values()
                .iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            // strMsg = strMsg + device.getDeviceName() + "\r\n";


            if (device.getVendorId() == 0x0471
                    && device.getProductId() == 0x5002) {
                mydevice = device;
				/*Toast.makeText(getApplicationContext(), "Find 0x0471",
						Toast.LENGTH_SHORT).show();
						debug("  find vendor=0x0471, productId=0x5002");*/
                strMsg = strMsg
                        + String.format(" STM32 OTG设备成功连接",
                        device.getVendorId(), device.getProductId());
                //debug(strMsg);
                Toast.makeText(MainActivity.this, strMsg, Toast.LENGTH_SHORT).show();
                break;

            }
        }
        if (mydevice == null) {
            Toast.makeText(getApplicationContext(),
                    "未找到STM32 Android OTG Device", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

		/*Toast.makeText(getApplicationContext(),
				"找到设备STM32 Android OTG Device", Toast.LENGTH_SHORT)
				.show();
		debug("find my usb device!");*/

        if (usbManager.hasPermission(mydevice)) { // 判断是否有权限 使用usb设备

            get_usb_info(); // 获取usb相关信息
			/*Toast.makeText(getApplicationContext(),
					"has permission,可以进行usb操作了", Toast.LENGTH_LONG)
					.show();
			debug(" have permission");*/
        } else {
            // Toast.makeText(getApplicationContext(), "No permission",
            // Toast.LENGTH_LONG).show();
            // 没有权限询问用户是否授予权限
            usbManager.requestPermission(mydevice, mPermissionIntent); // 该代码执行后，系统弹出一个对话框，
            // 询问用户是否授予程序操作USB设备的权限
        }
    }


    Handler usbMessageHandle = new Handler() { // 蓝牙消息 handler 对象
        public void handleMessage(final Message msg) {

            final byte[] InBuffer = (byte[]) msg.obj;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String crc;
                    String crcstr;
                    String stringBuffer = bytesToHexFun1(InBuffer);

                    Log.e("QQQ", "stringBuffer=" + stringBuffer);
                    String cmddate = stringBuffer.substring(2, 6);
                    String content = "";
                    switch (cmddate) {
                        case "0061":
                            content = stringBuffer.substring(0, 28);
                            break;
                        case "0062":
                            content = stringBuffer.substring(0, 18);
                            break;
                        case "0063":
                            content = stringBuffer.substring(0, 18);
                            break;
                        case "0064":
                            content = stringBuffer.substring(0, 20);
                            break;
                        case "0065":
                            content = stringBuffer.substring(0, 28);
                            break;
                        case "0066":
                            content = stringBuffer.substring(0, 18);
                            break;
                        case "0067":
                            content = stringBuffer.substring(0, 18);
                            break;

                    }
                    Log.e("TAG", "cmddate=" + cmddate);

                    txt_show.setText("content:" + content.toUpperCase());
                    //get_temp(stringBuffer);
                }
            });
        }
    };

    private void get_temp(String stringBuffer) {
        String crc;
        String crcstr;
        if (stringBuffer.length() >= 26) {
            crc = stringBuffer.substring(22, 26);
            Log.e("TAG", "crc=" + crc);
            crcstr = stringBuffer.substring(0, 22);
            Log.e("TAG", "crcstr=" + crcstr);
            String data1, data2;
            data1 = stringBuffer.substring(10, 18);//浮点温度值
            data2 = stringBuffer.substring(18, 22);//测量周期
            if (data2 != null && data2.equals("2000") || data2 != null && data2.equals("FFFF")) {//没有检测到物体
                return;
            }

            if (CrcUtil.flushLeft("0", 4, CrcUtil.setParamCRC(CrcUtil.hexStringToBytes(crcstr))).equals(crc.toUpperCase())) {
                Float value = Float.intBitsToFloat(Integer.valueOf(data1.trim(), 16));
                int distance = Integer.valueOf(data2.trim(), 16);
                //temperture = value;
                txt_show.setText("temperture:" + value + ",distance:" + distance);
            }
        }
    }


    void get_usb_info() {
        if (mydevice != null) {
            connection = usbManager.openDevice(mydevice); // 打开usb设备
            if (connection == null) { // 不成功，则退出
                // Toast.makeText(getApplicationContext(),
                // "usbManager.openDevice error", Toast.LENGTH_SHORT).show();
                //debug(" usb openDeivce() error.");
                return;
            }
        } else {
            return;
        }

        // Toast.makeText(getApplicationContext(), "usbManager.openDevice ok",
        // Toast.LENGTH_SHORT).show();
        //debug(" usbManager.openDevice ok");
        // if (!connection.claimInterface(intf, true))
        // return;

        //debug("Interface Count = " + mydevice.getInterfaceCount()); // 获取 usb
        // 接口数量
        if (mydevice.getInterfaceCount() != 1) {
            // Toast.makeText(getApplicationContext(),
            // "mydevice getInterfaceCount() error", Toast.LENGTH_SHORT).show();
            return;
        }
        // Toast.makeText(getApplicationContext(),
        // "mydevice getInterfaceCount() ok,count="+mydevice.getInterfaceCount(),
        // Toast.LENGTH_SHORT).show();

        intf = mydevice.getInterface(0); // 获取接口
        if (intf == null) {
            //debug(" intf = mydevice.getInterface(0) error");
        }
        connection.claimInterface(intf, true); // 独占接口

        // Toast.makeText(getApplicationContext(), "mydevice getInterface() ok",
        // Toast.LENGTH_SHORT).show();
        // 获取 endpoint
        int cnt = intf.getEndpointCount(); // 获取端点数
        //debug("intf.getEndpointCount = " + cnt);
        if (cnt < 1) {
            // Toast.makeText(getApplicationContext(),
            // "mydevice getEndpointCount() < 1", Toast.LENGTH_SHORT).show();
            //debug("mydevice getEndpointCount() < 1");
            return;
        }
        // String strMsg = new String();
        // strMsg.format("mydevice getEndpointCount() ok = %d ",
        // intf.getEndpointCount());
        // Toast.makeText(getApplicationContext(), strMsg,
        // Toast.LENGTH_SHORT).show();

        for (int index = 0; index < cnt; index++) {
            UsbEndpoint ep = intf.getEndpoint(index);
            if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                    && (ep.getDirection() == UsbConstants.USB_DIR_OUT)) {
                epOut = ep; // 针对主机而言，从主机 输出到设备,获取到 bulk的输出端点 对象
                // Toast.makeText(getApplicationContext(),
                // "mydevice get EndPoint epOut ", Toast.LENGTH_SHORT).show();
                //debug("mydevice get EndPoint epOut");
                // continue;
                Toast.makeText(MainActivity.this, "epOut", Toast.LENGTH_SHORT).show();
            }
            if ((ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
                    && (ep.getDirection() == UsbConstants.USB_DIR_IN)) {
                epIn = ep; // 针对主机而言，从设备 输出到主机 ,获取到 bulk的输入端点 对象
                //debug("mydevice get EndPoint epIn");
                // Toast.makeText(getApplicationContext(),
                // "mydevice get EndPoint epIn ", Toast.LENGTH_SHORT).show();
                // continue;
                Toast.makeText(MainActivity.this, "epIn", Toast.LENGTH_SHORT).show();
            }
        }

        // 创建线程 读取usb返回信息
        usb_Msg_Thread = new usbMsgThread(connection, epIn, usbMessageHandle); // 创建
        // usb数据
        // 接收线程
        usb_Msg_Thread.start();
        usb_used = 1;

    }


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        // usb 设备操作 授权
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent
                            .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // call method to set up device communication
                            if (mydevice == null) {
                                mydevice = device;
                                get_usb_info();
                            }
                            //debug(" usb permission granted.");
                            // Toast.makeText(getApplicationContext(),
                            // "permission enable for device ",
                            // Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Log.d(TAG, "permission denied for device " + device);
                        Toast.makeText(getApplicationContext(),
                                "permission denied for device ",
                                Toast.LENGTH_LONG).show();
                        //debug(" usb permission denied.");
                        return;
                    }
                }
            }
        }
    };

    public void onDestroy() {
        super.onDestroy();

        if (usb_used > 0) {
            connection.releaseInterface(intf); // 释放接口
            usb_Msg_Thread.interrupt(); // 结束 线程
        }
        Toast.makeText(getApplicationContext(), "USB host 测试应用程序 退出",
                Toast.LENGTH_LONG).show(); // 提示信息
    }

}
