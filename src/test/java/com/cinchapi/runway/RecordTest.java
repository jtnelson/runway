package com.cinchapi.runway;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.concourse.test.ClientServerTest;
import com.cinchapi.concourse.util.Random;
import com.cinchapi.runway.Record;
import com.cinchapi.runway.Required;
import com.cinchapi.runway.Unique;
import com.cinchapi.runway.json.JsonTypeWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class RecordTest extends ClientServerTest {

    private Runway runway;

    @Override
    protected String getServerVersion() {
        return "latest";
    }

    @Override
    public void beforeEachTest() {
        runway = Runway.connect("localhost", server.getClientPort(), "admin",
                "admin");
    }

    @Override
    public void afterEachTest() {
        try {
            runway.close();
        }
        catch (Exception e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    @Test
    public void testCannotAddDuplicateValuesForUniqueVariable() {
        Mock person = new Mock();
        person.name = "Jeff Nelson";
        Assert.assertTrue(runway.save(person));

        Mock person2 = new Mock();
        person2.name = "Jeff Nelson";
        Assert.assertFalse(runway.save(person2));

        person2.name = "Jeffery Nelson";
        Assert.assertTrue(runway.save(person2));
    }

    @Test
    public void testCannotSaveNullValueForRequiredVariable() {
        Mock person = new Mock();
        person.age = 23;
        Assert.assertFalse(runway.save(person));
    }

    @Test
    public void testNoPartialSaveWhenRequiredVariableIsNull() {
        Mock person = new Mock();
        person.age = 23;
        runway.save(person);
        Assert.assertTrue(client.describe(person.id()).isEmpty());
    }

    @Test
    public void testBooleanIsNotStoredAsBase64() {
        Mock person = new Mock();
        person.name = "John Doe";
        person.age = 100;
        runway.save(person);
        person = runway.load(Mock.class, person.id());
        Assert.assertTrue(person.alive);
    }

    @Test
    public void testSetDynamicAttribute() {
        Mock person = new Mock();
        person.set("0_2_0", "foo");
        System.out.println(person);
    }

    @Test
    public void testLoadPopulatesFields() {
        Mock person = new Mock();
        person.name = "Jeff Nelson";
        person.age = 100;
        runway.save(person);
        person = runway.load(Mock.class, person.id());
        Assert.assertEquals("Jeff Nelson", person.name);
        Assert.assertEquals((int) 100, (int) person.age);
    }

    @Test
    public void testLoadAllRecordsFromClass() {
        int count = Random.getScaleCount();
        for (int i = 0; i < count; ++i) {
            Mock mock = new Mock();
            mock.name = Random.getSimpleString();
            mock.age = Random.getInt();
            runway.save(mock);
        }
        Assert.assertEquals(count, runway.load(Mock.class).size());
    }

    @Test
    public void testCanGetReadablePrivateField() {
        Mock mock = new Mock();
        Assert.assertTrue(mock.map().containsKey("bar"));
        Assert.assertNotNull(mock.get("bar"));
    }

    @Test
    public void testCannotGetNonReadablePrivateField() {
        Mock mock = new Mock();
        Assert.assertFalse(mock.map().containsKey("foo"));
        Assert.assertNull(mock.get("foo"));
    }

    @Test(expected = Exception.class)
    public void testLoadNonExistingRecord() {
        System.out.println(runway.load(Mock.class, -2));
    }

    @Test
    public void testNoNoArgConstructor() {
        Flock flock = new Flock("Jeff Nelson");
        runway.save(flock); // TODO: change
        System.out.println(runway.load(Flock.class, flock.id()));
    }

    @Test
    public void testCustomTypeAdapter() {
        Sock sock = new Sock("sock", new Dock("dock"));
        Assert.assertTrue(sock.json().contains("foo"));
    }
    
    @Test
    public void testLoadRecordWithCollectionOfLinks() {
        Lock lock = new Lock(ImmutableList.of(new Dock("dock")));
        lock.save();
        Assert.assertEquals(lock, runway.load(Lock.class, lock.id()));
    }
    
    @Test
    public void testCircularLinks() {
        Tock tock = new Tock();
        Stock stock = new Stock();
        tock.stocks.add(stock);
        stock.tock = tock;
        tock.save();
        Assert.assertTrue(true);
    }

    class Mock extends Record {

        @Unique
        @Required
        public String name;

        public Integer age;

        public boolean alive = true;

        @SuppressWarnings("unused")
        private boolean foo = false;

        @Readable
        private boolean bar = false;

    }

    class Flock extends Record {

        public final String name;

        public Flock(String name) {
            this.name = name;
        }
    }

    class Sock extends Record {

        public final String sock;
        public final Dock dock;

        public Sock(String sock, Dock dock) {
            this.sock = sock;
            this.dock = dock;
        }
        
        @Override
        public Map<Class<?>, JsonTypeWriter<?>> jsonTypeWriters(){
            return ImmutableMap.of(Dock.class, (value) -> "foo");
        }

    }
    
    class Lock extends Record  {
        public final List<Dock> docks;
        
        public Lock(List<Dock> docks) {
            this.docks = docks;
        }
    }

    class Dock extends Record {

        public final String dock;

        public Dock(String dock) {
            this.dock = dock;
        }
    }
    
    class Tock extends Record {
        public List<Stock> stocks = Lists.newArrayList();
        
        public Tock() {
            
        }
    }
    
    class Stock extends Record {
        public Tock tock;
    }

}
