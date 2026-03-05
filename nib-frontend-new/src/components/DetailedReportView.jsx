import React from 'react';

const DetailedReportView = ({ report, dateRange }) => {
  return (
    <div className="content-card">
      {/* Report Header */}
      <div style={{ marginBottom: '25px', paddingBottom: '15px', borderBottom: '2px solid #e0e0e0' }}>
        <h2 style={{ margin: '0 0 5px 0', color: '#8B4513' }}>
          📋 {report.projectName}
        </h2>
        <p style={{ margin: '0', color: '#666' }}>
          {report.description}
        </p>
        <div style={{ display: 'flex', gap: '20px', marginTop: '10px', flexWrap: 'wrap' }}>
          <span><strong>Manager:</strong> {report.managerName}</span>
          <span><strong>Status:</strong> {report.status}</span>
          <span>
            <strong>RAG:</strong>{' '}
            <span style={{
              padding: '3px 10px',
              borderRadius: '4px',
              backgroundColor: report.ragStatus === 'GREEN' ? '#28a745' : 
                             report.ragStatus === 'AMBER' ? '#ffc107' : '#dc3545',
              color: report.ragStatus === 'AMBER' ? '#000' : '#fff',
              fontSize: '12px',
              fontWeight: '600'
            }}>
              {report.ragStatus}
            </span>
          </span>
          <span><strong>Period:</strong> {dateRange.startDate} to {dateRange.endDate}</span>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="stats-grid" style={{ marginBottom: '30px' }}>
        <SummaryCard label="Total Tasks" value={report.summary.totalTasks} />
        <SummaryCard label="Completed" value={report.summary.completedTasks} color="#28a745" />
        <SummaryCard label="Pending" value={report.summary.pendingTasks} color="#ffc107" />
        <SummaryCard label="Blocked" value={report.summary.blockedTasks} color="#dc3545" />
        <SummaryCard label="Completion" value={`${report.summary.completionPercentage}%`} 
                    color={report.summary.completionPercentage >= 80 ? '#28a745' : 
                          report.summary.completionPercentage >= 50 ? '#ffc107' : '#dc3545'} />
        <SummaryCard label="Updates" value={report.summary.totalUpdates} />
        <SummaryCard label="With Blockers" value={report.summary.updatesWithBlockers} color="#dc3545" />
        <SummaryCard label="Milestones" 
                    value={`${report.summary.milestonesCompleted}/${report.summary.milestonesTotal}`} 
                    color="#007bff" />
      </div>

      {/* Milestones Section */}
      {report.milestones?.length > 0 && (
        <Section title="🎯 Project Milestones" defaultOpen={true}>
          <div style={{ display: 'grid', gap: '12px' }}>
            {report.milestones.map(m => (
              <MilestoneItem key={m.id} milestone={m} />
            ))}
          </div>
        </Section>
      )}

      {/* Tasks Section */}
      {report.tasks?.length > 0 && (
        <Section title="✅ Tasks & Progress" defaultOpen={true}>
          <div style={{ display: 'grid', gap: '15px' }}>
            {report.tasks.map(task => (
              <TaskCard key={task.id} task={task} />
            ))}
          </div>
        </Section>
      )}

      {/* Daily Updates Timeline */}
      {report.dailyUpdates?.length > 0 && (
        <Section title="📅 Daily Progress Updates" defaultOpen={false}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {report.dailyUpdates.map(update => (
              <UpdateItem key={update.id} update={update} />
            ))}
          </div>
        </Section>
      )}
    </div>
  );
};


const SummaryCard = ({ label, value, color }) => (
  <div className="stat-card">
    <div className="stat-label">{label}</div>
    <div className="stat-value" style={{ color: color || 'inherit' }}>{value}</div>
  </div>
);

const Section = ({ title, children, defaultOpen = true }) => {
  const [open, setOpen] = React.useState(defaultOpen);
  return (
    <div style={{ marginBottom: '25px' }}>
      <button 
        onClick={() => setOpen(!open)}
        style={{
          width: '100%',
          padding: '12px 15px',
          background: '#f8f9fa',
          border: '1px solid #dee2e6',
          borderRadius: '6px',
          textAlign: 'left',
          fontWeight: '600',
          color: '#8B4513',
          cursor: 'pointer',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        {title}
        <span>{open ? '▼' : '▶'}</span>
      </button>
      {open && <div style={{ marginTop: '15px' }}>{children}</div>}
    </div>
  );
};

const MilestoneItem = ({ milestone }) => (
  <div style={{
    padding: '12px 15px',
    border: '1px solid #dee2e6',
    borderRadius: '6px',
    backgroundColor: milestone.isOverdue ? '#fff5f5' : '#f8f9fa'
  }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
      <div>
        <strong>{milestone.name}</strong>
        {milestone.description && <p style={{ margin: '5px 0', fontSize: '13px', color: '#666' }}>{milestone.description}</p>}
      </div>
      <span style={{
        padding: '4px 10px',
        borderRadius: '4px',
        fontSize: '11px',
        fontWeight: '600',
        backgroundColor: milestone.status === 'COMPLETED' ? '#28a745' : 
                        milestone.isOverdue ? '#dc3545' : '#ffc107',
        color: milestone.status === 'COMPLETED' || milestone.isOverdue ? '#fff' : '#000'
      }}>
        {milestone.status}
      </span>
    </div>
    <div style={{ fontSize: '12px', color: '#666', marginTop: '8px' }}>
      Target: {milestone.targetDate} 
      {milestone.completedDate && ` • Completed: ${milestone.completedDate}`}
      {milestone.isOverdue && !milestone.completedDate && 
        <span style={{ color: '#dc3545', fontWeight: '500' }}> • {Math.abs(milestone.daysUntilTarget)} days overdue</span>}
    </div>
  </div>
);

const TaskCard = ({ task }) => (
  <div style={{
    border: '1px solid #dee2e6',
    borderRadius: '8px',
    overflow: 'hidden'
  }}>
    <div style={{
      padding: '12px 15px',
      backgroundColor: '#f8f9fa',
      borderBottom: '1px solid #dee2e6',
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'start'
    }}>
      <div>
        <strong style={{ fontSize: '15px' }}>{task.taskName}</strong>
        {task.description && <p style={{ margin: '5px 0', fontSize: '13px', color: '#666' }}>{task.description}</p>}
        <div style={{ fontSize: '12px', color: '#666', marginTop: '5px' }}>
          Assigned to: {task.assignedToName} ({task.assignedToRole}) • 
          Due: {task.dueDate || 'Not set'}
          {task.isOverdue && <span style={{ color: '#dc3545', fontWeight: '500' }}> • OVERDUE</span>}
        </div>
      </div>
      <div style={{ textAlign: 'right' }}>
        <span style={{
          padding: '4px 10px',
          borderRadius: '4px',
          fontSize: '11px',
          fontWeight: '600',
          backgroundColor: task.status === 'COMPLETED' ? '#28a745' : 
                          task.status === 'BLOCKED' ? '#dc3545' : 
                          task.status === 'IN_PROGRESS' ? '#007bff' : '#6c757d',
          color: task.status === 'COMPLETED' || task.status === 'BLOCKED' ? '#fff' : '#fff'
        }}>
          {task.status}
        </span>
        {task.priority && (
          <div style={{ marginTop: '5px', fontSize: '11px' }}>
            Priority: <span style={{ color: task.priority >= 3 ? '#dc3545' : '#ffc107' }}>
              {task.priorityLabel}
            </span>
          </div>
        )}
      </div>
    </div>
    
    {/* Progress bar */}
    {task.completionPercentage != null && (
      <div style={{ padding: '8px 15px', backgroundColor: '#fff' }}>
        <div style={{ fontSize: '12px', marginBottom: '4px' }}>
          Progress: {task.completionPercentage}%
        </div>
        <div style={{ height: '6px', backgroundColor: '#e9ecef', borderRadius: '3px', overflow: 'hidden' }}>
          <div style={{
            width: `${task.completionPercentage}%`,
            height: '100%',
            backgroundColor: task.completionPercentage >= 80 ? '#28a745' : 
                            task.completionPercentage >= 50 ? '#ffc107' : '#dc3545',
            transition: 'width 0.3s ease'
          }} />
        </div>
      </div>
    )}
    
    {/* Updates for this task */}
    {task.updates?.length > 0 && (
      <div style={{ padding: '10px 15px', backgroundColor: '#f8f9fa', borderTop: '1px solid #dee2e6' }}>
        <div style={{ fontSize: '12px', fontWeight: '600', marginBottom: '8px', color: '#8B4513' }}>
          Recent Updates ({task.updates.length})
        </div>
        {task.updates.slice(0, 3).map(update => (
          <UpdateItem key={update.id} update={update} compact={true} />
        ))}
        {task.updates.length > 3 && (
          <div style={{ fontSize: '12px', color: '#007bff', textAlign: 'center', marginTop: '5px' }}>
            + {task.updates.length - 3} more updates in Daily Updates section
          </div>
        )}
      </div>
    )}
  </div>
);

const UpdateItem = ({ update, compact = false }) => (
  <div style={{
    padding: compact ? '8px 12px' : '12px 15px',
    border: '1px solid #e9ecef',
    borderRadius: '6px',
    backgroundColor: '#fff',
    marginBottom: compact ? '6px' : '10px'
  }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: '#666', marginBottom: '5px' }}>
      <span>📅 {update.updateDate}</span>
      <span>👤 {update.submittedByName} ({update.submittedByRole})</span>
    </div>
    {update.content && <p style={{ margin: '0 0 8px 0', fontSize: compact ? '13px' : '14px' }}>{update.content}</p>}
    {update.blockers && (
      <div style={{ 
        padding: '6px 10px', 
        backgroundColor: '#fff3cd', 
        borderRadius: '4px', 
        fontSize: '12px',
        color: '#856404',
        marginBottom: update.nextSteps ? '6px' : 0
      }}>
        ⚠️ Blockers: {update.blockers}
      </div>
    )}
    {update.nextSteps && (
      <div style={{ fontSize: '12px', color: '#004085' }}>
        ➡️ Next: {update.nextSteps}
      </div>
    )}
    {update.progressPercentage != null && (
      <div style={{ fontSize: '11px', color: '#666', marginTop: '5px' }}>
        Progress: {update.progressPercentage}%
      </div>
    )}
  </div>
);

export default DetailedReportView;