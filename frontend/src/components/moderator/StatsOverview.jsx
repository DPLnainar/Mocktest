import { Users, UserCheck, AlertTriangle, XCircle } from 'lucide-react'

export default function StatsOverview({ stats }) {
  const statCards = [
    {
      label: 'Total Students',
      value: stats.totalStudents,
      icon: Users,
      color: 'text-blue-400',
      bgColor: 'bg-blue-900/20',
    },
    {
      label: 'Active',
      value: stats.activeStudents,
      icon: UserCheck,
      color: 'text-green-400',
      bgColor: 'bg-green-900/20',
    },
    {
      label: 'Flagged (≥3 strikes)',
      value: stats.flaggedStudents,
      icon: AlertTriangle,
      color: 'text-yellow-400',
      bgColor: 'bg-yellow-900/20',
    },
    {
      label: 'Terminated',
      value: stats.terminatedStudents,
      icon: XCircle,
      color: 'text-red-400',
      bgColor: 'bg-red-900/20',
    },
  ]

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {statCards.map((stat, index) => (
        <div
          key={index}
          className={`${stat.bgColor} rounded-lg p-6 border border-gray-800`}
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-400 mb-1">{stat.label}</p>
              <p className={`text-3xl font-bold ${stat.color}`}>
                {stat.value}
              </p>
            </div>
            <stat.icon size={40} className={stat.color} />
          </div>
        </div>
      ))}
    </div>
  )
}
