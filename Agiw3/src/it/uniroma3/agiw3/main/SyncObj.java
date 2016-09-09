package it.uniroma3.agiw3.main;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**object used like regional condition
to sync the execution service semaphore and the slaves**/
public class SyncObj {
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition isSubmit = lock.newCondition();
	private boolean sub;

	public SyncObj(){
		this.sub=false;
	}

	/**P**/
	public void submit(){
		this.lock.lock();
		this.sub=true;
		this.isSubmit.signalAll();
		this.lock.unlock();
	}

	/**V**/
	public void check(){
		this.lock.lock();
		
		try{
			while(!this.sub){
				this.isSubmit.await();
			}
		} 
		catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		finally{
			this.lock.unlock();
		}
	}
}
