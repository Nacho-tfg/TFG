previous = 0
key="Fallo"
wifinet = "a"
pwd = "b"
uart.on("data")

function accessPoint()
    wifi.setmode(wifi.SOFTAP)
    cfg={}
    cfg.ssid="NodeMCU WiFi"
    cfg.auth= OPEN
    wifi.ap.config(cfg)
end

function createServer()
    srv = net.createServer(net.TCP, 30)
    srv:listen(7777, function(c)
    c:on("receive",
        function(c, pl)
            if (string.find(pl, "reg") ~= nil) then
                key = string.gsub(pl, "\n", "")
                key = string.gsub(key, "reg", "", 1)
                c:send("200\n")
            elseif (string.find(pl, "net") ~= nil) then
                wifinet = string.gsub(pl, "\n", "")
                wifinet = string.gsub(wifinet, "net", "", 1)
                c:send("200\n")
            elseif (string.find(pl, "pwd") ~= nil) then
                pwd = string.gsub(pl, "\n", "")
                pwd = string.gsub(pwd, "pwd", "", 1)
                c:send("200\n")
            else
                c:close()
                station()
            end
            print(pl)
            
        end)
    end)
end

function station()
    wifi.setmode(wifi.STATION)
    wifi.sta.config(wifinet, pwd)
    wifi.sta.connect()
        
    wifi.sta.eventMonReg(wifi.STA_GOTIP, 
        function()    
            setupUart()
            wifi.sta.eventMonStop(1)
        end)

    wifi.sta.eventMonStart()
            
    
end

function setupUart()
    uart.setup(0, 9600, 8, 0, 1, 1)
    uart.on("data", "\n",
        function(data)
            print("receive from uart:", data)
            if data == "a\n" then
                if previous == 0 then
                    previous = 1                       
                    json = createJson("Left")
                    sendPost(json)
                end
            elseif data == "b\n" then
                if previous == 1 then
                    previous = 0
                    json = createJson("Arrived")
                    sendPost(json)
                end
            end
        end, 0)
end

function createJson(message)
    ok, json = pcall(cjson.encode, {data={text=message},to=""..key..""})
    if ok then
        print("json ok!")
        print(string.len(json))
    else
        print("failed to encode!")
    end
    return json
end

function sendPost(message)
    conn=net.createConnection(net.TCP, false)
    conn:on("connection", 
        function(conn) 
            print("connected")
            conn:send("POST /fcm/send HTTP/1.1\r\n"
                    .."Host: fcm.googleapis.com:443\r\n"
                    .."Content-Type:application/json\r\n"
                    .."Authorization:key=\r\n"
                    .."Content-Length: "..string.len(message).."\r\n"
                    .."\r\n"
                    ..message,
                         function()
                            print("POST sent")
                            conn:close()
                         end)
        end)
    conn:on("disconnection", function(conn)  print("disconnected") end )
    conn:on("receive", function(conn, payload) print("on receive:\n"..payload) end)
    conn:connect(80, "fcm.googleapis.com")
end

accessPoint()
createServer()
