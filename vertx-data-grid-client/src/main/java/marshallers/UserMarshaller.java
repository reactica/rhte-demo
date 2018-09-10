package marshallers;


import com.redhat.coderland.reactica.model.User;
import org.infinispan.protostream.MessageMarshaller;

import java.io.IOException;

public class UserMarshaller implements MessageMarshaller<User> {


  @Override
  public User readFrom(ProtoStreamReader reader) throws IOException {
    User user = new User();
    user.setId(reader.readString("id"));
    user.setName(reader.readString("name"));
    user.setRideId(reader.readString("reactica"));
    user.setCurrentState(reader.readString("currentState"));
    user.setEnterTime(reader.readLong("enterTime"));

    return null;
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, User user) throws IOException {
    writer.writeString("id",user.getId());
    writer.writeString("name",user.getName());
    writer.writeString("rideId","reactica");
    writer.writeString("currentState",user.getCurrentState());
    writer.writeLong("enterTime",user.getEnterTime());

  }

  @Override
  public Class<? extends User> getJavaClass() {
    return User.class;
  }

  @Override
  public String getTypeName() {
    return "com.redhat.coderland.reactica.model.User";
  }
}
