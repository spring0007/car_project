package com.awell.library;

public class AwellTool {

    /**
     * 接收的具体方法状态（用户接收）
     */
    public static final String STATUS_ACCEPT = "awellStatusAccept";

    /**
     * 发送的具体方法状态（用户发送）（无返回值的返回null或""）
     */
    public static final String STATUS_SEND = "awellStatusSend";

    /**
     * String类型默认值
     */
    public static final String DEFAULT_S = "default";

    /**
     * int类型默认值
     */
    public static final int DEFAULT_I = -1;

    /**
     * （STATUS_SEND）返回值多数据时以SPLIT分隔
     */
    public static final String SPLIT = "###";


    public static final String IS_EXIT = "_exit";

    public static final int NO_OPEN = 0;
    public static final int OPEN = 0x10;

    /**
     * 媒体播放 返回三个参数（STATUS_ACCEPT）
     * VALUE_M1(String)   当前播放媒体包名
     * VALUE_M2(String)   当前播放媒体状态 "start" 此包名开始  "stop" 此包名结束
     * VALUE_M3(int)      当前播放媒体音频类型 AudioManager.STREAM_MUSIC 等
     */
    public static final String MEDIA_PLAY = "mediaPlay";

    /**
     * 锁屏状态通知 返回一个参数（STATUS_ACCEPT）
     * VALUE_M1(boolean)  true 进入锁屏  false 退出锁屏
     */
    public static final String LOCK_SCREEN_STATUS = "lockScreenStatus";



    /**
     * 背光开关 带一个参数（STATUS_SEND）
     * VALUE_M1(boolean)  true 打开背光  false 关闭背光
     */
    public static final String BACK_LIGHT = "backLight";

    /**
     * 获取亮度最大值最小值 （STATUS_SEND）
     * return 最小亮度 SPLIT 最大亮度 SPLIT 当前亮度值
     * eq : return min + SPLIT + max + SPLIT + brightness
     */
    public static final String GET_BRIGHTNESS = "getBrightness";

    /**
     * 设置亮度（STATUS_SEND）
     * VALUE_M1(int) 亮度值
     */
    public static final String SET_BRIGHTNESS = "setBrightness";

    /**
     * 打开应用 带两个参数（STATUS_SEND）
     * VALUE_M1(String)  包名
     * VALUE_M2(String)  类名(不知道时可不带)
     * return true 打开  false  应用不存在或其他异常
     */
    public static final String START_APP = "startApp";

    /**
     * 关闭应用 带两个参数（STATUS_SEND）
     * VALUE_M1(String)  包名
     * VALUE_M2(String)  类名(不知道时可不带)
     * return true 关闭  false  应用不存在或其他异常
     */
    public static final String STOP_APP = "stopApp";

    /**
     * 关闭当前应用
     * 用于 （STOP_APP） 的 （VALUE_M1）参数
     */
    public static final String CLOSE_CURRENT_APP = "close.current.application";

    /**
     * 锁屏待机 带一个参数（STATUS_SEND）
     * VALUE_M1(boolean)  true 进入锁屏  false 退出锁屏
     */
    public static final String LOCK_SCREEN = "lockScreen";

    /**
     * 打开关闭右视 带一个参数（STATUS_SEND）
     * VALUE_M1(boolean)  true 进入右视  false 退出右视
     */
    public static final String RIGHT_VIDEO = "rightVideo";

    /**
     * CPU温度 带一个参数（STATUS_SEND）
     * VALUE_M1(float)
     */
    public static final String CPU_TMEP = "cpu_temperature";

    public class RADIO{

        /**
         * 收音机包名
         */
        public static final String PACKAGE_NAME_RADIO = "com.awell.radio";

        /**
         * 收音机频率  返回三个参数 （STATUS_ACCEPT）
         * VALUE_M1(String)   FM or AM
         * VALUE_M2(String)  具体频率 87.5 or 531
         * VALUE_M3(String)  单位 MHz or KHz
         */
        public static final String FREQUENCY = PACKAGE_NAME_RADIO + "_frequency";

        /**
         * 后台播放当前收音机频率（后台打开收音机）（STATUS_SEND）
         */
        public static final String PLAY = PACKAGE_NAME_RADIO + "_play";

        /**
         * 收音机上一曲 （STATUS_SEND）
         */
        public static final String PREVIOUS = PACKAGE_NAME_RADIO + "_previous";

        /**
         * 收音机下一曲 （STATUS_SEND）
         */
        public static final String NEXT = PACKAGE_NAME_RADIO + "_next";

        /**
         * 收音机进步上 （STATUS_SEND）
         */
        public static final String DECREASE = PACKAGE_NAME_RADIO + "_decrease";

        /**
         * 收音机进步下 （STATUS_SEND）
         */
        public static final String INCREASE = PACKAGE_NAME_RADIO + "_increase";

        /**
         * 收音机上搜索 （STATUS_SEND）
         */
        public static final String PREV_STATION = PACKAGE_NAME_RADIO + "_prevStation";

        /**
         * 收音机下搜索 （STATUS_SEND）
         */
        public static final String NEXT_STATION = PACKAGE_NAME_RADIO + "_nextStation";

        /**
         * 收音机设置FM AM （STATUS_SEND）
         */
        public static final String SET_FMAM = PACKAGE_NAME_RADIO + "_setFmAm";

        /**
         * 收音机设置FM AM   带一个参数（STATUS_SEND）
         * VALUE_M1(int) FM or AM   FM : 0  AM : 1
         */
        public static final String SET_FMAM1 = PACKAGE_NAME_RADIO + "_setFmAm1";

        /**
         * 收音机设置Loc DX（STATUS_SEND）
         */
        public static final String SET_LocDX = PACKAGE_NAME_RADIO + "_setLocDX";

        /**
         * 收音机设置Loc DX   带一个参数（STATUS_SEND）
         * VALUE_M1(int) Loc or DX   Loc : 0  DX : 1
         */
        public static final String SET_LocDX1 = PACKAGE_NAME_RADIO + "_setLocDX1";

        /**
         * 全搜索 存台（STATUS_SEND）
         */
        public static final String AUTO_SCAN = PACKAGE_NAME_RADIO + "_autoScan";

        /**
         * 指定频率 87.5 or 531（STATUS_SEND）
         * VALUE_M1(String) "87.5"  "87.6"  "531" 等等
         */
        public static final String TUNE_STATION = PACKAGE_NAME_RADIO + "_tuneStation";

        /**
         * 退出收音机（STATUS_SEND）
         */
        public static final String EXIT = PACKAGE_NAME_RADIO + "_exit";

        /**
         * 收音机列表上一页（STATUS_SEND）
         */
        public static final String LIST_PRE_PAGE = PACKAGE_NAME_RADIO + "_listPrePage";

        /**
         * 收音机列表下一页（STATUS_SEND）
         */
        public static final String LIST_NEXT_PAGE = PACKAGE_NAME_RADIO + "_listNextPage";

        /**
         * 收音机列表序号1（STATUS_SEND）
         */
        public static final String LIST_NUM1 = PACKAGE_NAME_RADIO + "_listNum1";

        /**
         * 收音机列表序号2（STATUS_SEND）
         */
        public static final String LIST_NUM2 = PACKAGE_NAME_RADIO + "_listNum2";

        /**
         * 收音机列表序号3（STATUS_SEND）
         */
        public static final String LIST_NUM3 = PACKAGE_NAME_RADIO + "_listNum3";

        /**
         * 收音机列表序号4（STATUS_SEND）
         */
        public static final String LIST_NUM4 = PACKAGE_NAME_RADIO + "_listNum4";

        /**
         * 收音机列表序号5（STATUS_SEND）
         */
        public static final String LIST_NUM5 = PACKAGE_NAME_RADIO + "_listNum5";
        /**
         * 收音机列表序号6（STATUS_SEND）
         */
        public static final String LIST_NUM6 = PACKAGE_NAME_RADIO + "_listNum6";
    }

    public class MUSIC{

        /**
         * 音乐包名
         */
        public static final String PACKAGE_NAME_MUSIC = "com.awell.localmusic";

        /**
         * 音乐播放状态 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(boolean)  true 正在播放  false 没播放
         */
        public static final String PLAY_STATUS = PACKAGE_NAME_MUSIC + "_playStatus";

        /**
         * 音乐播放数据 返回三个参数（STATUS_ACCEPT）
         * VALUE_M1(String)  歌曲名称
         * VALUE_M2(String)  歌手名称
         * VALUE_M3(String)  专辑
         */
        public static final String PLAY_NAME = PACKAGE_NAME_MUSIC + "_playName";

        /**
         * 音乐播放进度 返回两个参数（STATUS_ACCEPT）
         * VALUE_M1(long)  当期时间 单位毫秒
         * VALUE_M2(long)  总时间 单位毫秒
         */
        public static final String PLAY_TIME = PACKAGE_NAME_MUSIC + "_playTime";

        /**
         * 音乐播放歌词 返回两个参数（STATUS_ACCEPT）
         * VALUE_M1(String[])  歌词数组
         * VALUE_M2(int)  当前播放歌词下标
         */
        public static final String PLAY_LYRICS = PACKAGE_NAME_MUSIC + "_playLyrics";

        /**
         * 音乐专辑图片 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(String)  专辑图片路径
         */
        public static final String PLAY_IMAGE = PACKAGE_NAME_MUSIC + "_playImage";

        /**
         * 音乐播放模式 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(int)   0 循环播放  1 单曲循环  2 随机播放
         */
        public static final String PLAY_MODEL = PACKAGE_NAME_MUSIC + "_playModel";

        /**
         * 播放本地音乐（STATUS_SEND）
         */
        public static final String PLAY = PACKAGE_NAME_MUSIC + "_play";

        /**
         * 本地音乐上一曲 （STATUS_SEND）
         */
        public static final String PREVIOUS = PACKAGE_NAME_MUSIC + "_previous";

        /**
         * 本地音乐下一曲 （STATUS_SEND）
         */
        public static final String NEXT = PACKAGE_NAME_MUSIC + "_next";

        /**
         * 暂停本地音乐 （STATUS_SEND）
         */
        public static final String PAUSE = PACKAGE_NAME_MUSIC + "_pause";

        /**
         * 播放/暂停本地音乐 （STATUS_SEND）
         */
        public static final String PLAY_PAUSE = PACKAGE_NAME_MUSIC + "_playPause";

        /**
         * 获取本地音乐信息 （STATUS_SEND）
         * return  歌曲名 SPLIT 歌手 SPLIT 专辑
         */
        public static final String MUSIC_INFO = PACKAGE_NAME_MUSIC + "_musicInfo";

        /**
         * 获取当前本地音乐状态 （STATUS_SEND）
         *  return  false 没在播放  true 正在播放
         */
        public static final String GET_STATE = PACKAGE_NAME_MUSIC + "_getState";

        /**
         * 获获取当前音乐播放模式 （STATUS_SEND）
         *  return 0 循环播放  1 单曲循环  2 随机播放
         */
        public static final String GET_MODEL = PACKAGE_NAME_MUSIC + "_getModel";

        /**
         * 设置本地音乐播放模式 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 循环播放  1 单曲循环  2 随机播放
         */
        public static final String SET_MODEL = PACKAGE_NAME_MUSIC + "_setModel";

        /**
         * 退出本地音乐（STATUS_SEND）
         */
        public static final String EXIT = PACKAGE_NAME_MUSIC + "_exit";
    }

    public class VIDEO{
        /**
         * 视频包名
         */
        public static final String PACKAGE_NAME_VIDEO = "com.awell.localvideo";

        /**
         * 播放本地视频（STATUS_SEND）
         */
        public static final String PLAY = PACKAGE_NAME_VIDEO + "_play";

        /**
         * 本地视频上一曲 （STATUS_SEND）
         */
        public static final String PREVIOUS = PACKAGE_NAME_VIDEO + "_previous";

        /**
         * 本地视频下一曲 （STATUS_SEND）
         */
        public static final String NEXT = PACKAGE_NAME_VIDEO + "_next";

        /**
         * 暂停本地视频 （STATUS_SEND）
         */
        public static final String PAUSE = PACKAGE_NAME_VIDEO + "_pause";

        /**
         * 播放/暂停本地视频 （STATUS_SEND）
         */
        public static final String PLAY_PAUSE = PACKAGE_NAME_VIDEO + "_playPause";
    }

    public class BT{
        /**
         * 蓝牙包名
         */
        public static final String PACKAGE_NAME_BT = "com.awell.bluetooth";

        /**
         * 蓝牙播放状态 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(boolean)  true 正在播放  false 没播放
         */
        public static final String PLAY_STATUS = PACKAGE_NAME_BT + "_playStatus";

        /**
         * 蓝牙播放数据 返回三个参数（STATUS_ACCEPT）
         * VALUE_M1(String)  歌曲名称
         * VALUE_M2(String)  歌手名称
         * VALUE_M3(String)  专辑
         */
        public static final String PLAY_NAME = PACKAGE_NAME_BT + "_playName";

        /**
         * 音乐播放进度 返回两个参数（STATUS_ACCEPT）
         * VALUE_M1(int)  当期时间 单位秒
         * VALUE_M2(int)  总时间 单位秒
         */
        public static final String PLAY_TIME = PACKAGE_NAME_BT + "_playTime";

        /**
         * 蓝牙播放歌词 返回两个参数（STATUS_ACCEPT）
         * VALUE_M1(String[])  歌词数组
         * VALUE_M2(int)  当前播放歌词下标
         */
        public static final String PLAY_LYRICS = PACKAGE_NAME_BT + "_playLyrics";

        /**
         * 蓝牙状态 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(int)  0 关  1 未连接  2 已连接
         */
        public static final String BT_STATE = PACKAGE_NAME_BT + "_btState";

        /**
         * 蓝牙来电信息 返回三个参数（STATUS_ACCEPT）
         * VALUE_M1(String)  电话号码
         * VALUE_M2(String)  名称
         * VALUE_M3(int)  0接听  2去电  4来电  7挂断
         */
        public static final String CONTACT_INFO = PACKAGE_NAME_BT + "_contactInfo";

        /**
         * 联系人信息 返回一个参数（STATUS_ACCEPT）
         * VALUE_M1(String)  json格式
         */
        public static final String ALL_CONTACT = PACKAGE_NAME_BT + "_allContact";

        /**
         * 播放蓝牙音乐（STATUS_SEND）
         */
        public static final String PLAY = PACKAGE_NAME_BT + "_play";

        /**
         * 本地蓝牙上一曲 （STATUS_SEND）
         */
        public static final String PREVIOUS = PACKAGE_NAME_BT + "_previous";

        /**
         * 本地蓝牙下一曲 （STATUS_SEND）
         */
        public static final String NEXT = PACKAGE_NAME_BT + "_next";

        /**
         * 暂停蓝牙音乐 （STATUS_SEND）
         */
        public static final String PAUSE = PACKAGE_NAME_BT + "_pause";

        /**
         * 播放/暂停本地视频 （STATUS_SEND）
         */
        public static final String PLAY_PAUSE = PACKAGE_NAME_BT + "_playPause";

        /**
         * 获取蓝牙音乐信息 （STATUS_SEND）
         * return  歌曲名 SPLIT 歌手 SPLIT 专辑
         */
        public static final String MUSIC_INFO = PACKAGE_NAME_BT + "_musicInfo";

        /**
         * 获取当前蓝牙音乐状态 （STATUS_SEND）
         *  return  false 没在播放  true 正在播放
         */
        public static final String GET_STATE = PACKAGE_NAME_BT + "_getState";

        /**
         * 获取当前蓝牙状态 （STATUS_SEND）
         *  return  0 关  1 未连接  2 已连接
         */
        public static final String GET_BT_STATE = PACKAGE_NAME_BT + "_getBtState";

        /**
         * 开关蓝牙 带一个参数（STATUS_SEND）
         * VALUE_M1(boolean)  true 打开蓝牙  false 关闭蓝牙
         */
        public static final String SET_BT_STATE = PACKAGE_NAME_BT + "_setBtState";

        /**
         * 拨打某某电话 （STATUS_SEND）
         * VALUE_M1(String)  电话号码 或 联系人名称
         * return true 存在  false 不存在
         */
        public static final String PHONE = PACKAGE_NAME_BT + "_phoneCall";

        /**
         * 接听电话 （STATUS_SEND）
         */
        public static final String ANSWER = PACKAGE_NAME_BT + "_answerCall";

        /**
         * 挂断电话（STATUS_SEND）
         */
        public static final String HANGUP = PACKAGE_NAME_BT + "_hangupCall";

        /**
         * 下载返回联系人信息（STATUS_SEND）
         */
        public static final String CONTACTS = PACKAGE_NAME_BT + "_downLoadContacts";

        /**
         * 设置蓝牙连接模式（STATUS_SEND）
         * VALUE_M1(int)  0 自动匹配  1 密码匹配
         */
        public static final String SET_PAIR_MODE = PACKAGE_NAME_BT + "_setPairMode";

        /**
         * 退出蓝牙（STATUS_SEND）
         */
        public static final String EXIT = PACKAGE_NAME_BT + "_exit";

        /**
         * 获取蓝牙本地地址（STATUS_SEND）
         * return string地址
         */
        public static final String LOCAL_ADDRESS = PACKAGE_NAME_BT + "_localAddress";

        /**
         * 获取已连接蓝牙设备的地址（STATUS_SEND）
         * return string地址
         */
        public static final String CONNECT_ADDRESS = PACKAGE_NAME_BT + "_connectAddress";

        /**
         * 断开蓝牙连接（STATUS_SEND）
         * return true 成功 false 失败
         */
        public static final String DISCONNECT = PACKAGE_NAME_BT + "_disconnect";

        /**
         * 获取蓝牙名称（STATUS_SEND）
         * return string名称
         */
        public static final String GET_NAME = PACKAGE_NAME_BT + "_getName";

        /**
         * 蓝牙IAP连接状态（STATUS_SEND）
         * return
         */
        public static final String GET_IAP = PACKAGE_NAME_BT + "_getIAP";

    }

    public class MEDIA{

        /**
         * 当前媒体播放（STATUS_SEND）
         */
        public static final String PLAY = "mediaMediaPlay";

        /**
         * 当前媒体上一曲 （STATUS_SEND）
         */
        public static final String PREVIOUS = "mediaMediaPrevious";

        /**
         * 当前媒体下一曲 （STATUS_SEND）
         */
        public static final String NEXT = "mediaMediaNext";

        /**
         * 当前媒体暂停 （STATUS_SEND）
         */
        public static final String PAUSE = "mediaMediaPause";

        /**
         * 当前媒体播放/暂停 （STATUS_SEND）
         */
        public static final String PLAY_PAUSE = "mediaMediaPlayPause";
    }

    public class CANBUS{
        /**
         * 车辆信息
         */
        public static final String PACKAGE_NAME_CANBUS = "com.awell.canbus";

        /**
         * 空调开关 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭空调  1 打开空调
         */
        public static final String AIR_POWER = PACKAGE_NAME_CANBUS + "_airPower";

        /**
         * 空调同步 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭空调同步  1 空调同步
         */
        public static final String AIR_SYNC = PACKAGE_NAME_CANBUS + "_airSync";

        /**
         * 吹前窗吹身吹脚 带一个参数（STATUS_SEND）
         * VALUE_M1(int)(111 吹前窗 吹身 吹脚)  0 关闭  1 吹脚  2 吹身  3 吹身吹脚  4 吹前窗  5 吹前窗吹脚  6 吹前窗吹身 7 吹前窗吹身吹脚
         */
        public static final String AIR_WIND = PACKAGE_NAME_CANBUS + "_airWind";

        /**
         * 自动模式 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭自动模式  1 自动模式
         */
        public static final String AIR_AUTO = PACKAGE_NAME_CANBUS + "_airAuto";

        /**
         * 前除霜 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭前除霜  1 打开前除霜  2 最大前除霜
         */
        public static final String AIR_FRONT = PACKAGE_NAME_CANBUS + "_airFront";

        /**
         * 后除霜 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭后除霜  1 打开后除霜
         */
        public static final String AIR_REAR = PACKAGE_NAME_CANBUS + "_airRear";

        /**
         * 内外循环 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 外循环  1 内循环  2 自动内外循环
         */
        public static final String AIR_CIRCLE = PACKAGE_NAME_CANBUS + "_airCircle";

        /**
         * 制冷 A/C 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭A/C  1 打开A/C  2 最大 A/C  3 最大制热
         */
        public static final String AIR_AC = PACKAGE_NAME_CANBUS + "_airAc";

        /**
         * 左温区温度调节 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  1 增加温度  2 减小温度  3 最高温度  4 最低温度
         */
        public static final String AIR_TEMP_LEFT = PACKAGE_NAME_CANBUS + "_airTempLeft";

        /**
         * 左温区温度调节，指定温度 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  指定温度*2
         * eq: 温度调到 20℃  VALUE_M1 = 40， 温度调到 20.5℃  VALUE_M1 = 41
         */
        public static final String AIR_TEMP_LEFT1 = PACKAGE_NAME_CANBUS + "_airTempLeft1";

        /**
         * 右温区温度调节 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  1 增加温度  2 减小温度  3 最高温度  4 最低温度
         */
        public static final String AIR_TEMP_RIGHT = PACKAGE_NAME_CANBUS + "_airTempRight";

        /**
         * 右温区温度调节，指定温度 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  指定温度*2
         * eq: 温度调到 20℃  VALUE_M1 = 40， 温度调到 20.5℃  VALUE_M1 = 41
         */
        public static final String AIR_TEMP_RIGHT1 = PACKAGE_NAME_CANBUS + "_airTempRight1";

        /**
         * 风速调节 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  1 调高风速  2 调低风速  3 最大风速  4 最低风速
         */
        public static final String AIR_WIND_SPEED = PACKAGE_NAME_CANBUS + "_airWindSpeed";

        /**
         * 风速调节，指定风速 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  风速等级
         */
        public static final String AIR_WIND_SPEED1 = PACKAGE_NAME_CANBUS + "_airWindSpeed1";

        /**
         * 天窗 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭天窗  1 打开天窗  2 天窗开一半  3 天窗关一半
         */
        public static final String AIR_SKYLIGHT = PACKAGE_NAME_CANBUS + "_skylight";

        /**
         * 遮阳帘 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭遮阳帘  1 打开遮阳帘  2 遮阳帘开一半  3 遮阳帘关一半
         */
        public static final String AIR_SUNSHADE = PACKAGE_NAME_CANBUS + "_sunshade";

        /**
         * 风速最大值最小值查询 （STATUS_SEND）
         * return  最小值MIN SPLIT 最大值MAX SPLIT 当前风速
         */
        public static final String AIR_WIND_SPEED_INQUIRE = PACKAGE_NAME_CANBUS + "_windSpeedInquire";

        /**
         * 左温度最大值最小值查询 （STATUS_SEND）
         * return  最小值MIN SPLIT 最大值MAX SPLIT 左当前温度
         */
        public static final String AIR_TEMP_LEFT_INQUIRE = PACKAGE_NAME_CANBUS + "_tempLeftInquire";

        /**
         * 右温度最大值最小值查询 （STATUS_SEND）
         * return  最小值MIN SPLIT 最大值MAX SPLIT 右当前温度
         */
        public static final String AIR_TEMP_RIGHT_INQUIRE = PACKAGE_NAME_CANBUS + "_tempRightInquire";

        /**
         * 空调控制（STATUS_SEND）
         * VALUE_M1(int)  key 空调功能
         * VALUE_M2(int)  指定值
         */
        public static final String AIR_CONTROL = PACKAGE_NAME_CANBUS + "_airData";


    }

    public class PURIFIER{

        /**
         * 净化器
         */
        public static final String PACKAGE_NAME_PURIFIER = "com.awell.cleanup";

        /**
         * 打开负离子/打开净化器（STATUS_SEND）
         */
        public static final String OPEN = PACKAGE_NAME_PURIFIER + "_open";

        /**
         * 关闭负离子/关闭净化器（STATUS_SEND）
         */
        public static final String CLOSE = PACKAGE_NAME_PURIFIER + "_close";

        /**
         * 打开空气净化/帮我空气净化（STATUS_SEND）
         */
        public static final String OPEN_AIR_PURIFY = PACKAGE_NAME_PURIFIER + "_open_air_purify";

        /**
         * 关闭空气净化（STATUS_SEND）
         */
        public static final String CLOSE_AIR_PURIFY = PACKAGE_NAME_PURIFIER + "_close_air_purify";

        /**
         * 打开去异味/帮我去异味（STATUS_SEND）
         */
        public static final String OPEN_deodorize = PACKAGE_NAME_PURIFIER + "_open_deodorize";

        /**
         * 关闭去异味（STATUS_SEND）
         */
        public static final String CLOSE_deodorize = PACKAGE_NAME_PURIFIER + "_close_deodorize";

    }

    public class MONITOTING {

        /**
         * 全景、监控
         */
        public static final String PACKAGE_NAME_MONITOTING = "com.awell.lz360";

        /**
         * 监控/全景 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭全景  1 打开全景  2 打开/关闭全景
         */
        public static final String PANORAMA = PACKAGE_NAME_MONITOTING + "_panorama";

        /**
         * 前视 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭前视  1 打开前视  2 打开/关闭前视
         */
        public static final String FRONT = PACKAGE_NAME_MONITOTING + "_front";

        /**
         * 后视 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭后视  1 打开后视  2 打开/关闭后视
         */
        public static final String REAR = PACKAGE_NAME_MONITOTING + "_rear";

        /**
         * 左视 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭左视  1 打开左视  2 打开/关闭左视
         */
        public static final String LEFT = PACKAGE_NAME_MONITOTING + "_left";

        /**
         * 右视 带一个参数（STATUS_SEND）
         * VALUE_M1(int)  0 关闭右视  1 打开右视  2 打开/关闭右视
         */
        public static final String RIGHT = PACKAGE_NAME_MONITOTING + "_right";

    }

    /**
     * VALUE
     */
    public static final String VALUE_M1 = "value_m1";
    public static final String VALUE_M2 = "value_m2";
    public static final String VALUE_M3 = "value_m3";
    public static final String VALUE_M4 = "value_m4";
    public static final String VALUE_M5 = "value_m5";
    public static final String VALUE_M6 = "value_m6";

    /**
     * 返回与SPLIT区分的固定格式
     * 第一位为 个数length
     * 第二位为 值
     */
    public static final String[][] ReturnSplit = {{"3",GET_BRIGHTNESS},
            {"3",MUSIC.MUSIC_INFO},{"3",BT.MUSIC_INFO},{"3",CANBUS.AIR_WIND_SPEED_INQUIRE},
            {"3",CANBUS.AIR_TEMP_LEFT_INQUIRE},{"3",CANBUS.AIR_TEMP_RIGHT_INQUIRE}};

}
