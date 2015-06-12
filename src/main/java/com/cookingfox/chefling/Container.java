package com.cookingfox.chefling;

import com.cookingfox.chefling.command.*;
import com.cookingfox.chefling.exception.ChildCannotBeDefaultException;
import com.cookingfox.chefling.exception.ContainerException;

import java.util.HashMap;
import java.util.Map;

/**
 * @see ContainerInterface
 */
public class Container implements ContainerInterface {

    //----------------------------------------------------------------------------------------------
    // PROTECTED PROPERTIES
    //----------------------------------------------------------------------------------------------

    /**
     * Stores operation commands, where the key is the command class and the value is the instance.
     */
    protected final Map<Class, Object> commands = new HashMap<Class, Object>();

    /**
     * Stores created instances, where the key is the type and the value is the instance. This
     * instance is returned the next time the type is requested.
     */
    protected final Map<Class, Object> instances = new HashMap<Class, Object>();

    /**
     * Stores type mappings, where the key is the type and the value is the mapping provided by the
     * `map...` methods.
     */
    protected final Map<Class, Object> mappings = new HashMap<Class, Object>();

    /**
     * A collection of child Containers.
     */
    protected final ContainerChildren children = new ContainerChildren();

    /**
     * Convenience singleton for apps using a process-wide Container instance.
     */
    protected static volatile Container defaultInstance;

    //----------------------------------------------------------------------------------------------
    // CONSTRUCTOR
    //----------------------------------------------------------------------------------------------

    /**
     * Default constructor: initializes the Container.
     */
    public Container() {
        initialize();
    }

    //----------------------------------------------------------------------------------------------
    // PUBLIC METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * @see ContainerInterface#create(Class)
     */
    @Override
    public <T> T create(Class<T> type) throws ContainerException {
        return getCommand(CreateCommand.class).create(type);
    }

    /**
     * @see ContainerInterface#get(Class)
     */
    @Override
    public <T> T get(Class<T> type) throws ContainerException {
        return getCommand(GetCommand.class).get(type);
    }

    /**
     * @see ContainerInterface#has(Class)
     */
    @Override
    public boolean has(Class type) {
        if (type == null) {
            return false;
        }

        return instances.containsKey(type) ||
                mappings.containsKey(type) ||
                children.hasChildFor(type);
    }

    /**
     * @see ContainerInterface#mapFactory(Class, Factory)
     */
    @Override
    public <T> void mapFactory(Class<T> type, Factory<T> factory) throws ContainerException {
        getCommand(MapFactoryCommand.class).mapFactory(type, factory);
    }

    /**
     * @see ContainerInterface#mapInstance(Class, Object)
     */
    @Override
    public <T> void mapInstance(Class<T> type, T instance) throws ContainerException {
        getCommand(MapInstanceCommand.class).mapInstance(type, instance);
    }

    /**
     * @see ContainerInterface#mapType(Class, Class)
     */
    @Override
    public <T> void mapType(Class<T> type, Class<? extends T> subType) throws ContainerException {
        getCommand(MapTypeCommand.class).mapType(type, subType);
    }

    /**
     * @see ContainerInterface#remove(Class)
     */
    @Override
    public void remove(Class type) throws ContainerException {
        // first remove from children containers
        children.remove(type);

        // remove from self
        getCommand(RemoveCommand.class).remove(type);
    }

    /**
     * @see ContainerInterface#reset()
     */
    @Override
    public void reset() {
        // reset children containers first
        children.reset();

        // reset self
        getCommand(ResetCommand.class).reset();

        // clear commands and reinitialize
        commands.clear();
        initialize();
    }

    /**
     * Adds a child Container, which contains its own unique configuration.
     *
     * @param child The child Container.
     * @throws ContainerException
     */
    @Override
    public void addChild(Container child) throws ContainerException {
        if (defaultInstance != null && child == defaultInstance) {
            throw new ChildCannotBeDefaultException();
        }

        getCommand(AddChildCommand.class).addChild(child);
    }

    /**
     * Convenience singleton for apps using a process-wide Container instance.
     *
     * @return Default Container instance.
     */
    public static Container getDefault() {
        if (defaultInstance == null) {
            synchronized (Container.class) {
                if (defaultInstance == null) {
                    defaultInstance = new Container();
                }
            }
        }

        return defaultInstance;
    }

    //----------------------------------------------------------------------------------------------
    // PROTECTED METHODS
    //----------------------------------------------------------------------------------------------

    /**
     * Get one of the operation commands by its type.
     *
     * @param commandClass Class of the command.
     * @param <T>          Ensures the instance is cast to the expected type.
     * @return A command instance.
     */
    @SuppressWarnings("unchecked")
    protected <T> T getCommand(Class<T> commandClass) {
        return (T) commands.get(commandClass);
    }

    /**
     * Initializes the container.
     */
    protected void initialize() {
        ContainerHelper containerHelper = new ContainerHelper(this, instances, mappings, children);

        // create operation commands
        commands.put(AddChildCommand.class, new AddChildCommand(containerHelper));
        commands.put(CreateCommand.class, new CreateCommand(containerHelper));
        commands.put(GetCommand.class, new GetCommand(containerHelper));
        commands.put(MapFactoryCommand.class, new MapFactoryCommand(containerHelper));
        commands.put(MapInstanceCommand.class, new MapInstanceCommand(containerHelper));
        commands.put(MapTypeCommand.class, new MapTypeCommand(containerHelper));
        commands.put(RemoveCommand.class, new RemoveCommand(containerHelper));
        commands.put(ResetCommand.class, new ResetCommand(containerHelper));

        // map this instance to its class and interface
        instances.put(Container.class, this);
        instances.put(ContainerInterface.class, this);
    }

}
