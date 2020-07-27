package com.meeple.backend.input;

import java.util.List;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;

public class PlayerInput {
	public static class CommandQueue<Command, Action extends Runnable> {
		private Supplier<Set<Action>> setSupplier = new CollectionSuppliers.SetSupplier<>();

		Queue<Command> queuedCommands = new ConcurrentLinkedQueue<>();
		Map<Command, Set<Action>> actions = new HashMap<>();

		public void queue(Command command) {
			this.queuedCommands.offer(command);
		}

		public void add(Command command, Action action) {
			FrameUtils.addToSetMap(actions, command, action, setSupplier);
		}

		public void remove(Command command, Action action) {
			Set<Action> set = actions.get(command);
			if (set != null && !set.isEmpty()) {
				set.remove(action);
			}
		}

		public void process() {
			synchronized (queuedCommands) {
				for (Iterator<Command> i = queuedCommands.iterator(); i.hasNext();) {
					Command command = i.next();
					Set<Runnable>  actionCollection = (Set<Runnable>) actions.get(command);
					FrameUtils.iterateRunnable(actionCollection, false);
					i.remove();
				}
			}

		}

	}

	class ControlBase<UserInput, Command> {
		Map<UserInput, List<Command>> in;
		Map<Command, List<Runnable>> out;
	}

	class Controller<UserInput, Command> extends ControlBase<UserInput, Command> {

		Set<ControllerSubModule<UserInput, Command>> subModules;
		Set<Command> queuedCommands;

		public void in(UserInput input) {
			List<Command> commands = in.get(input);
			for (ControllerSubModule<UserInput, Command> sub : subModules) {
				commands.addAll(sub.in.get(input));
			}
			for (Command command : commands) {
				this.queuedCommands.add(command);
			}
		}

		public void out() {
			synchronized (queuedCommands) {
				for (Iterator<Command> i = queuedCommands.iterator(); i.hasNext();) {
					Command command = i.next();
					FrameUtils.iterateRunnable(out.get(command), false);
					for (ControllerSubModule<UserInput, Command> sub : subModules) {
						FrameUtils.iterateRunnable(sub.out.get(command), false);
					}
					i.remove();
				}
			}
		}

	}

	class ControllerSubModule<UserInput, Command> extends ControlBase<UserInput, Command> {

	}

	void keyPress(int key, boolean repeatOrSingle, String command) {

	}

	void bindCommand(String command, Runnable reciever) {

	}

}
