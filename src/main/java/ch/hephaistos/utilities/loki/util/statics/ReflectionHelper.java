/**
 * The MIT License
 *
 * Copyright (c) 2019 Ricardo Daniel Monteiro Simoes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ch.hephaistos.utilities.loki.util.statics;

import ch.hephaistos.utilities.loki.util.interfaces.ChangeListener;
import ch.hephaistos.utilities.loki.util.interfaces.ObjectChangeListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A small helper for Reflection.
 *
 * @author I-Al-Istannen, : https://github.com/I-Al-Istannen
 */
public class ReflectionHelper {

    private static List<ChangeListener> interfacesToInvoke = new LinkedList<ChangeListener>();


    public static void addInterfaceToUpdate(ChangeListener interfaceToAdd){
        interfacesToInvoke.add(interfaceToAdd);
    }

    /**
     * Returns all fields from the class and all superclasses.
     *
     * @param start  The start {@link Class}
     * @param filter A filter for the fields, in order to only return some
     *               results.
     * @return All fields matching the {@link Predicate} from the whole class
     * hierarchy.
     */
    public static List<Field> getAllFieldsInClassHierachy(Class<?> start, Predicate<Field> filter) {
        List<Field> fields = new ArrayList<>();
        Class<?> currentClass = start;

        while (currentClass != null) {
            Collections.addAll(fields, currentClass.getDeclaredFields());
            currentClass = currentClass.getSuperclass();
        }

        return fields.stream()
                .filter(filter)
                .collect(Collectors.toList());
    }

    /**
     * Returns the value of a field.
     *
     * @param field  The {@link Field} to get the value from.
     * @param handle The handle object to use (according to
     *               {@link Field#get(Object)})
     * @param <T>    The type of the return value you expect
     * @return The value of the field.
     * @throws ClassCastException        if the type is not what you stored it as
     * @throws ReflectionHelperException if any
     *                                   {@link ReflectiveOperationException} occurs.
     */
    public static <T> T getFieldValue(Field field, Object handle) {
        try {
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            T t = (T) field.get(handle);
            return t;
        } catch (ReflectiveOperationException e) {
            throw new ReflectionHelperException(e);
        }
    }

    /**
     * Sets the newValue of a field.
     * This function calls the objects that implemented the corresponding listeners.
     * The Listeners get called the following way:
     *
     * 1. ObjectChangeListener
     * 2. ChangeListener
     *
     * This means, that after a newValue has been changed using reflection, the update function of the object gets
     * called first.
     *
     * This is so that one can make sure the object runs its necessary functions before the GUI starts
     * updating itself.
     *
     * @param field  The {@link Field} to set the newValue for.
     * @param object The object object to use (according to
     * {@link Field#set(Object, Object)})
     * @param newValue  The newValue to set it to
     * @throws ReflectionHelperException if any
     * {@link ReflectiveOperationException} occurs.
     */
    public static void setFieldValue(Field field, Object object, Object newValue) {
        try {
            field.setAccessible(true);
            Object oldValue = field.get(object);
            field.set(object, newValue);

            if(object instanceof ObjectChangeListener){
                ((ObjectChangeListener) object).onFieldValueChanged(field);
            }


            notifyListeners(field, oldValue, newValue, object);

        } catch (ReflectiveOperationException e) {
            throw new ReflectionHelperException(e);
        }
    }


    /**
     * This function is called when the value of a variable changes. Use this to launch
     * updates or changes in other parts of the application.
     *
     * First thing it does is notify all the interfaces.
     * Due to there being a risk of an unhandled exception with the Reflective invokation of the update method,
     * they are done last so that at least the interfaces can receive an update.
     *
     * @param oldValue The previous value of the field
     * @param newValue The new value for the field
     * @param object the object that had its value changed
     * @param field is the field that was changed
     */
    public static void notifyListeners(Field field, Object oldValue, Object newValue, Object object){
        for(ChangeListener i : interfacesToInvoke){i.onObjectValueChanged(field, oldValue, newValue, object);}
    }

    public static class ReflectionHelperException extends RuntimeException {
        ReflectionHelperException(Throwable cause) {
            super(cause);
        }
    }
}
