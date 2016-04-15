package edu.hm.cs.vss.remote;

import edu.hm.cs.vss.Chair;
import edu.hm.cs.vss.Philosopher;
import edu.hm.cs.vss.Table;
import edu.hm.cs.vss.TableMaster;
import edu.hm.cs.vss.remote.event.ChairMessage;
import edu.hm.cs.vss.remote.event.Message;
import edu.hm.cs.vss.remote.event.TableMasterMessage;
import edu.hm.cs.vss.remote.event.TableMessage;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Fabio Hellmann on 14.04.2016.
 */
public class RemoteTable implements Table {
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public RemoteTable(final String host) throws IOException {
        final Socket socket = new Socket(host, 8888);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void addTable(final String tableHost) {
        send(new TableMessage(Message.Type.ADD_TABLE, tableHost));
    }

    @Override
    public void removeTable(final String tableHost) {
        send(new TableMessage(Message.Type.DELETE_TABLE, tableHost));
    }

    @Override
    public Stream<Table> getTables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Chair> getChairs() {
        final Optional<ChairMessage> response = sendAndGet(new ChairMessage(Message.Type.GET_CHAIRS));
        if(response.isPresent() && response.get().getParam().isPresent()) {
            return response.get().getParam().get().stream();
        }
        return Stream.empty();
    }

    @Override
    public Chair getNeighbourChair(Chair chair) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTableMaster(TableMaster tableMaster) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TableMaster getTableMaster() {
        final Optional<TableMasterMessage> response = sendAndGet(new TableMasterMessage(Message.Type.GET_MIN_MEAL_COUNT));
        if(response.isPresent() && response.get().getParam().isPresent()) {
            final int minMealCount = response.get().getParam().get();
            return (TableMaster) philosopher -> philosopher.getMealCount() <= minMealCount;
        }
        return (TableMaster) philosopher -> true;
    }

    private void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> sendAndGet(T message) {
        try {
            out.writeObject(message);
            out.flush();

            return Optional.of((T) in.readObject());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
