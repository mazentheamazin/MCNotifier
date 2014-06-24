/*
* Copyright (C) 2014 Mazen K.
* This file is part of MCNotifier.
*
* MCNotifier for Bukkit is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 to be exact
*
* MCNotifier is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with MCNotifier. If not, see <http://www.gnu.org/licenses/>.
*/

package io.mazenmc.notifier.server;

import io.mazenmc.notifier.Notifier;
import io.mazenmc.notifier.NotifierPlugin;
import io.mazenmc.notifier.client.NotifierClient;
import io.mazenmc.notifier.packets.PacketLoginError;
import io.mazenmc.notifier.packets.PacketLoginSuccess;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class NotifierServer extends Thread{

    private ServerSocket server;

    public NotifierServer() throws IOException {
        server = new ServerSocket(5932);

        server.setSoTimeout(Integer.MAX_VALUE);

        NotifierPlugin.getPlugin().getLogger().info("Server on port 5932 has been started");
    }

    @Override
    public void run() {
        while(true) {
            Socket socket;
            try{
                socket = server.accept();

                DataOutputStream oos = new DataOutputStream(socket.getOutputStream());
                DataInputStream ois = new DataInputStream(socket.getInputStream());

                //Run auth. process
                String[] d = ois.readUTF().split(":");

                String username = d[0];
                char[] password = d[1].toCharArray();

                if(!NotifierPlugin.getSettingsManager().getUserData().containsKey(username)) {
                    oos.writeUTF(new PacketLoginError(Notifier.generatePacketArgs("Username/password is incorrect")).toString());
                    oos.flush();
                    continue;
                }

                if(!Arrays.equals(NotifierPlugin.getSettingsManager().getUserData().get(username), password)) {
                    oos.writeUTF(new PacketLoginError(Notifier.generatePacketArgs("Username/password is incorrect!")).toString());
                    oos.flush();
                    continue;
                }

                oos.writeUTF(new PacketLoginSuccess(Notifier.emptyPacketArgs()).toString());
                oos.flush();

                new NotifierClient(socket, ois, oos, username);

                NotifierPlugin.getPlugin().getLogger().info(username + " has logged in!");
            }catch(SocketTimeoutException ex) {
                try{
                    server.close();

                    server = new ServerSocket(5932);
                    server.setSoTimeout(Integer.MAX_VALUE);
                }catch(Exception e) {e.printStackTrace();}

                ex.printStackTrace();
            }catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
